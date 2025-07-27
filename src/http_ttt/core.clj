(ns http-ttt.core
  (:require [http-ttt.tttHandler :refer :all])
  (:import (Server Server ServerArgs)
           [http_ttt.tttHandler TttHandler]))



(defn -main [& args]
  (let [args-array (into-array String args)
        config (ServerArgs. args-array)
        server (Server. config)]
    (if (.isHelpRequested config )
      (System/exit 0)
      (do
        (.addRoute server "/ttt" (TttHandler.))
        (.start server)
        ))
    ))