(ns http-ttt.core
  (:require [http-ttt.tttHandler :refer :all]
            [tic-tac-toe.psql :as pg]
            [tic-tac-toe.edn]
            [tic-tac-toe.persistence]
            [tic-tac-toe.human-turn]
            [tic-tac-toe.ai-turn])
  (:import (Server Server ServerArgs)
           [http_ttt.tttHandler TttHandler]))

(defn handler-factory [store]
  (TttHandler. store))

(defn server-factory [config]
  (Server. config))


(defn -main [& args]
  (let [flags (set args)
        store (cond
                (flags "-file") :file
                (flags "-psql") (do (pg/db-setup) :psql)
                :else :mem)
        args-array (into-array String args)
        config (ServerArgs. args-array)
        server (server-factory config)]
    (if (.isHelpRequested config)
      (System/exit 0)
      (do
        (.addRoute server "/ttt\\?*.*" (handler-factory store))
        (.start server)))))
