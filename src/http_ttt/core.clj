(ns http-ttt.core
  (:require [tic-tac-toe.board :as board])
  (:import (Server ServerArgs Server)))


(defn -main [& args]
  (let [args-array (into-array String args)
        config (ServerArgs. args-array)
        server (Server. config)]
    (if (.isHelpRequested config )
      (System/exit 0)
      (do
        (.start server)
        (prn (board/get-board :3x3))))
    ))