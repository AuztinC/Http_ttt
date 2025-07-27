(ns http-ttt.web-view)


(defmulti render-screen :screen)

(defmethod render-screen :select-game-mode [_state]
  "<h1>Select Game Mode</h1>")