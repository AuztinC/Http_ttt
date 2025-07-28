(ns http-ttt.tttHandler
  (:require [clojure.string :as str]
            [http-ttt.render-screen :refer [render-screen]]
            [http-ttt.transition :as transition])
  (:import (Server StatusCode)
           (Server.HTTP HttpResponse)
           (Server.Routes RouteHandler)))

(defn parse-query-params [path]
  (when (str/includes? path "?")
    (let [[_ query-string] (str/split path #"\?" 2)]
      (->> (str/split query-string #"&")
        (map #(str/split % #"=" 2))
        (into {})))))


(deftype TttHandler [store]
  RouteHandler
  (handle [_this req]
    (let [path (String. (.getPath req))
          query (parse-query-params path)
          state {:store  store
                 :screen :select-game-mode}
          updated-state (if-let [choice (get query "choice")]
                          (transition/transition state choice)
                          state)
          html (render-screen updated-state)]
      (HttpResponse. (. StatusCode valueOf "OK") "text/html" (byte-array (.getBytes html))))))