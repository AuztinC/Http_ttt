(ns http-ttt.transition)

(defmulti transition :screen)

(defmethod transition :select-game-mode [state choice]
  (case choice
    "1" (assoc state :players [:human :ai] :screen :select-board)
    "2" (assoc state :players [:ai :human] :screen :select-board)
    "3" (assoc state :players [:human :human] :screen :select-board)
    "4" (assoc state :players [:ai :ai] :screen :select-board)))