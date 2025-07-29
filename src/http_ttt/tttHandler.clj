(ns http-ttt.tttHandler
  (:require [clojure.string :as str]
            [http-ttt.render-screen :refer [render-screen]]
            [http-ttt.transition :as transition]
            [tic-tac-toe.board :as board]
            [tic-tac-toe.game :as game]
            [tic-tac-toe.persistence :as db])
  (:import (Server StatusCode)
           (Server.HTTP HttpRequest HttpResponse)
           (Server.Routes RouteHandler)))
;; TODO ARC - get starting screens working
;; TODO ARC - continue game - replay
(defn- determine-starting-screen [store query]
  (cond
    (db/in-progress? {:store store}) :in-progress-game
    (db/previous-games? {:store store}) :replay-confirm
    :else (keyword (get query "screen" "select-game-mode"))))

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

(defn query-state [cookie-map store query]
  (let [game-from-cookie (if (and (get cookie-map "game") (not (str/blank? (get cookie-map "game"))))
                           (read-string (get cookie-map "game")))
         state-from-db (when (:id game-from-cookie) (db/find-game-by-id {:store store} (:id game-from-cookie)))]
    (or state-from-db
      game-from-cookie
      {:store        store
       :ui           :web
       :screen       (determine-starting-screen store query)
       :players      (parse-players (get query "players"))
       :board-size   (first (parse-players (get query "board-size")))
       :difficulties (parse-players (get query "difficulties"))
       :turn         "p1"
       :markers      ["X" "O"]})))

(defn handle-request
  [{:keys [store path cookies]}]
  (let [query (parse-query-params path)
        cookie-map (parse-cookies cookies)
        state (query-state cookie-map store query)

        human-move (if-let [choice (get query "choice")]
                     (transition/transition state choice)
                     state)
        next-player (case (:turn human-move)
                      "p1" (first (:players human-move))
                      "p2" (second (:players human-move)))
        ai-move (if (and (= :game (:screen human-move)) (= :ai next-player))
                  (game/next-state human-move)
                  human-move)
        final-state (if (and (:board ai-move) (board/check-winner (:board ai-move)))
                      (assoc ai-move :screen :game-over)
                      ai-move)
        html (render-screen final-state)
        set-cookie? (and
                      (:set-cookie? human-move))]
    {:state       final-state
     :html        html
     :set-cookie? true}))
;; TODO ARC - clear cookie game over

(deftype TttHandler [store]
  RouteHandler
  (handle [_ req]
    (let [path (String. (.getPath req))
          cookies (try (.getHeader req "cookie") (catch Exception _ nil))
          {:keys [state html set-cookie?]} (handle-request {:store store :path path :cookies cookies})
          body (byte-array (.getBytes html))
          header (if set-cookie?
                   (byte-array (.getBytes (str "Set-Cookie: game=" state "; Path=/\r\nContent-Type: text/html\r\n"))))
          statusOK (. StatusCode valueOf "OK")]
      (HttpResponse. statusOK header body))))
