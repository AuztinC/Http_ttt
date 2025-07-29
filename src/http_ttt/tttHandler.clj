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

(defn parse-hyphens [param]
  (when param (vec (map keyword (str/split param #"-")))))

(defn- parse-cookies [^String cookie-header]
  (when cookie-header
    (->> (str/split cookie-header #";")
      (map str/trim)
      (map #(str/split % #"=" 2))
      (into {}))))

(defn query-state [cookie-map store query]
  (let [id-str (get cookie-map "gameId")
        game-id (when (and id-str (not (str/blank? id-str)))
                  (Integer/parseInt id-str))
        state-from-db (when game-id
                        (db/find-game-by-id {:store store} game-id))
        full-state-cookie (when-let [g (get cookie-map "game")]
                            (read-string g))]
    (or state-from-db
      full-state-cookie
      {:store        store
       :ui           :web
       :screen       (determine-starting-screen store query)
       :players      (parse-hyphens (get query "players"))
       :board-size   (keyword (get query "board-size"))
       :difficulties (parse-hyphens (get query "difficulties"))
       :turn         "p1"
       :markers      ["X" "O"]})))

(defn handle-request
  [{:keys [store path cookies]}]
  (let [query (parse-query-params path)
        cookie-map (parse-cookies cookies)
        state (query-state cookie-map store query)
        next-state (if-let [choice (get query "choice")]
                     (transition/handle-screen state choice)
                     state)
        next-player (case (:turn next-state)
                      "p1" (first (:players next-state))
                      "p2" (second (:players next-state)))
        ai-move (if (and (= :game (:screen next-state)) (= :ai next-player))
                  (game/next-state next-state)
                  next-state)
        final-state (if (and (:board ai-move) (board/check-winner (:board ai-move)))
                      (assoc ai-move :screen :game-over)
                      ai-move)
        html (render-screen final-state)]
    {:state       final-state
     :html        html
     :set-cookie? true}))

(deftype TttHandler [store]
  RouteHandler
  (handle [_ req]
    (let [path (String. (.getPath req))
          cookies (try (.getHeader req "cookie") (catch Exception _ nil))
          {:keys [state html]} (handle-request {:store store :path path :cookies cookies})
          body (byte-array (.getBytes html))
          header (cond
                   (= :game-over (:screen state))
                   (byte-array (.getBytes (str "Set-Cookie: game=; Max-Age=0; Path=/\r\n" "Set-Cookie: gameId=; Max-Age=0; Path=/\r\nContent-Type: text/html\r\n")))
                   (:id state)
                   (byte-array (.getBytes (str "Set-Cookie: game=" state "; Path=/" "Set-Cookie: gameId=" (:id state) "; Path=/\r\nContent-Type: text/html\r\n")))
                   :else (byte-array (.getBytes (str "Set-Cookie: game=" state "; Path=/\r\nContent-Type: text/html\r\n"))))
          statusOK (. StatusCode valueOf "OK")]
      (HttpResponse. statusOK header body))))
