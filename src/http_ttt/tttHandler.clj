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
        ;game-id (when game-id-str (Integer/parseInt game-id-str))
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
        _ (prn "player " next-player)
        next-state (if (= :game (:screen human-move))
                     (cond
                       (= :ai next-player) (game/next-state human-move)
                       (and
                         (:board human-move)
                         (board/check-winner (:board human-move)))
                       (assoc human-move :screen :game-over)
                       :else human-move)
                     human-move)
        html (render-screen next-state)
        set-cookie? (and
                      (:set-cookie? human-move))]
    ;(prn "cookie state" (read-string game-str))
    {:state       next-state
     :html        html
     :set-cookie? true}))
;; TODO ARC - clear cookie game over
;(let [player (case (:turn state)
;                   "p1" (first (:players state))
;                   "p2" (second (:players state)))]
;      (cond
;        (= [:human :human] [(first (:players state)) (second (:players state))])
;        (if (board/check-winner (:board state))
;          (assoc state :screen :game-over)
;          state)
;
;        (= :ai player) (game/next-state state)
;
;        :else state))

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
