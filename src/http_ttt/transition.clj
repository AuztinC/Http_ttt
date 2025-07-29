(ns http-ttt.transition
  (:require [tic-tac-toe.board :as board]
            [tic-tac-toe.game :as game]
            [tic-tac-toe.persistence :as db]))

(defn new-game? [state]
  (let [board (:board state)]
    (if (nil? board)
      false
      (let [empty-count (case (:board-size state)
                          :3x3 9
                          :4x4 16
                          :3x3x3 27
                          :default nil)]
        (= empty-count (count (board/open-positions board)))))))

(defmulti handle-screen (fn [state _] (:screen state)))

(defmethod handle-screen :select-game-mode [state choice]
  (case choice
    "1" (assoc state :players [:human :ai] :screen :select-board)
    "2" (assoc state :players [:ai :human] :screen :select-board)
    "3" (assoc state :players [:human :human] :screen :select-board)
    "4" (assoc state :players [:ai :ai] :screen :select-board)))

(defmethod handle-screen :select-board [state choice]
  (case choice
    "1" (assoc state :board-size :3x3 :board (board/get-board :3x3) :screen :select-difficulty)
    "2" (assoc state :board-size :4x4 :board (board/get-board :4x4) :screen :select-difficulty)
    "3" (assoc state :board-size :3x3x3 :board (board/get-board :3x3x3) :screen :select-difficulty)))

(defmethod handle-screen :select-difficulty [state choice]
  (let [diff (case choice
               "1" :easy
               "2" :medium
               "3" :hard)
        ai-count (count (filterv #(= :ai %) (:players state)))
        updated-difficulties (conj (vec (:difficulties state)) diff)]
    (if (< (count updated-difficulties) ai-count)
      (assoc state :difficulties updated-difficulties
        :screen :select-difficulty)
      (do
        (db/clear-active {:store (:store state)})
        (-> state
          (assoc :id (db/set-new-game-id {:store (:store state)})
            :difficulties updated-difficulties
            :screen :game
            :board (board/get-board (:board-size state))
            :turn "p1"
            :markers ["X" "O"]
            :set-cookie? true))))))

(defmethod handle-screen :game [state choice]
  (let [marker (if (= (:turn state) "p1")
                 (first (:markers state))
                 (second (:markers state)))
        idx (Integer/parseInt choice)
        updated-state (if (new-game? state)
                        (assoc state :board (assoc (:board state) idx [marker]) :turn (game/next-player (:turn state)) )
                        (assoc state :board (assoc (:board state) idx [marker]) :turn (game/next-player (:turn state))))
        empty? (= "" (first (nth (:board state) idx)))]
    (if empty?
      (do
        (db/update-current-game! updated-state idx)
        (assoc state :board (assoc (:board state) idx [marker])
          :screen :game
          :turn (game/next-player (:turn state))))
      state)))

(defmethod handle-screen :game-over [state _choice]
  (let [winner (board/check-winner (:board state))]
    (prn "state -" state)
    (prn "winner -" winner)
    ))