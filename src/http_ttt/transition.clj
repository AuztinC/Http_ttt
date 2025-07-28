(ns http-ttt.transition
  (:require [tic-tac-toe.persistence :as db]))

;; TODO ARC - add game settings as params until game in progress

(defmulti transition :screen)

(defmethod transition :select-game-mode [state choice]
  (case choice
    "1" (assoc state :players [:human :ai] :screen :select-board)
    "2" (assoc state :players [:ai :human] :screen :select-board)
    "3" (assoc state :players [:human :human] :screen :select-board)
    "4" (assoc state :players [:ai :ai] :screen :select-board)))

(defmethod transition :select-board [state choice]
  (case choice
    "1" (assoc state :board-size :3x3 :screen :select-difficulty)
    "2" (assoc state :board-size :4x4 :screen :select-difficulty)
    "3" (assoc state :board-size :3x3x3 :screen :select-difficulty)))


(defmethod transition :select-difficulty [state choice]

  (let [diff (case choice
               "1" :easy
               "2" :medium
               "3" :hard)
        ai-count (count (filterv #(= :ai %) (:players state)))
        updated-difficulties (conj (vec (:difficulties state)) diff)
        _ (prn state)]
    (prn "ai count" ai-count)
    (if (< (count updated-difficulties) ai-count)
      (assoc state :difficulties updated-difficulties
        :screen :select-difficulty)
      (do
        (db/clear-active {:store (:store state)})
        (-> state
          (assoc :id (db/set-new-game-id {:store (:store state)})
            :difficulties updated-difficulties
            :screen :game))))))