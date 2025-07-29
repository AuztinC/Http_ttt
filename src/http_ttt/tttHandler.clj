(ns http-ttt.tttHandler
  (:require [clojure.string :as str]
            [http-ttt.render-screen :refer [render-screen]]
            [http-ttt.transition :as transition]
            [tic-tac-toe.board :as board]
            [tic-tac-toe.persistence :as db])
  (:import (Server StatusCode)
           (Server.HTTP HttpRequest HttpResponse)
           (Server.Routes RouteHandler)))

(defn- determine-starting-screen [store]
  (cond
    (db/in-progress? {:store store}) :in-progress-game
    (db/previous-games? {:store store}) :replay-confirm
    :else :select-game-mode))

(defn parse-query-params [path]
  (when (str/includes? path "?")
    (let [[_ query-string] (str/split path #"\?" 2)]
      (->> (str/split query-string #"&")
        (map #(str/split % #"=" 2))
        (into {})))))

(defn parse-players [param]
  (when param (vec (map keyword (str/split param #"-")))))

(defn new-game? [state]
  (let [board (:board state)]
    (if (nil? board)
      false
      (let [empty-count (case (:board-size state)
                          :3x3 9
                          :4x4 16
                          :3x3x3 27
                          :default nil)]
        (= empty-count (count (board/open-positions board)))))))

(defn- parse-cookies [^String cookie-header]
  (when cookie-header
    (->> (str/split cookie-header #";")
      (map str/trim)
      (map #(str/split % #"=" 2))
      (into {}))))

(defn handle-request
  [{:keys [store path cookies]}]
  (let [query (parse-query-params path)
        cookie-map (parse-cookies cookies)
        game-from-cookie (if (and (get cookie-map "game") (not (str/blank? (get cookie-map "game"))))
                           (read-string (get cookie-map "game")))
        ;game-id (when game-id-str (Integer/parseInt game-id-str))
        state-from-db (when (:id game-from-cookie) (db/find-game-by-id {:store store} (:id game-from-cookie)))
        base-state (or state-from-db
                     game-from-cookie
                     {:store        store
                      ;:screen       (determine-starting-screen store)
                      :players      (parse-players (get query "players"))
                      :board-size   (first (parse-players (get query "board-size")))
                      :difficulties (parse-players (get query "difficulties"))})
        screen (keyword (get query "screen" "select-game-mode"))
        pre-trans (assoc base-state :screen screen)
        updated-state (if-let [choice (get query "choice")]
                        (transition/transition pre-trans choice)
                        pre-trans)
        final-state (if (and (:board updated-state) (board/check-winner (:board updated-state)))
                      (assoc updated-state :screen :game-over)
                      updated-state)
        html (render-screen final-state)
        set-cookie? (and
                      (:set-cookie? updated-state)
                      (not game-from-cookie))]
    ;(prn "cookie state" (read-string game-str))
    {:state       final-state
     :html        html
     :set-cookie? true
     :cookie-value final-state}))


(deftype TttHandler [store]
  RouteHandler
  (handle [_ req]
    (let [path (String. (.getPath req))
          cookies (try (.getHeader req "cookie") (catch Exception _ nil))
          {:keys [state html set-cookie? cookie-value]} (handle-request {:store store :path path :cookies cookies})
          body (byte-array (.getBytes html))
          header (if set-cookie?
                   (byte-array (.getBytes (str "Set-Cookie: game=" cookie-value "; Path=/\r\nContent-Type: text/html\r\n"))))
          statusOK (. StatusCode valueOf "OK")]
      (HttpResponse. statusOK header body))))
