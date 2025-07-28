(ns http-ttt.tttHandler
  (:require [clojure.string :as str]
            [http-ttt.render-screen :refer [render-screen]]
            [http-ttt.transition :as transition]
            [tic-tac-toe.board :as board])
  (:import (Server StatusCode)
           (Server.HTTP HttpResponse)
           (Server.Routes RouteHandler)))

(defn parse-query-params [path]
  (when (str/includes? path "?")
    (let [[_ query-string] (str/split path #"\?" 2)]
      (->> (str/split query-string #"&")
        (map #(str/split % #"=" 2))
        (into {})))))

;; TODO ARC - add cookie current game ID on init

(defn parse-players [param]
  (when param (vec (map keyword (str/split param #"-")))))

(deftype TttHandler [store]
  RouteHandler
  (handle [_this req]
    (let [path (String. (.getPath req))
          query (parse-query-params path)
          _ (prn query)
          base-state {:store        store
                      :players      (parse-players (get query "players"))
                      :board-size   (first (parse-players (get query "board-size")))
                      :board        (board/get-board :board-size)
                      :difficulties (parse-players (get query "difficulties"))}
          initial-screen (keyword (get query "screen" "select-game-mode"))
          pre-transition (assoc base-state :screen initial-screen)
          updated-state (if-let [choice (get query "choice")]
                          (transition/transition pre-transition choice)
                          pre-transition)
          html (render-screen updated-state)]
      (HttpResponse. (. StatusCode valueOf "OK") "text/html" (byte-array (.getBytes html))))))