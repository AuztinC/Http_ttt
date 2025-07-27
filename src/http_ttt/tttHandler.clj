(ns http-ttt.tttHandler
  (:require [http-ttt.web-view :refer [render-screen]])
  (:import (Server StatusCode)
           (Server.HTTP HttpResponse)
           (Server.Routes RouteHandler)))

(deftype TttHandler []
  RouteHandler
  (handle [_this _req]
    (let [initial-state {:screen :select-game-mode}
          html (render-screen initial-state)]
      (HttpResponse. (. StatusCode valueOf "OK") "text/html" (byte-array (.getBytes html))))))