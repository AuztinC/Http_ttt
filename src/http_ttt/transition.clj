(ns http-ttt.transition
  (:require [tic-tac-toe.board :as board]
            [tic-tac-toe.game :as game]
            [tic-tac-toe.persistence :as db]))

(defmulti handle-screen (fn [state _] (:screen state)))

(defmethod handle-screen :select-game-mode [state query]
  (case (get query "choice")
    "1" (assoc state :players [:human :ai] :screen :select-board)
    "2" (assoc state :players [:ai :human] :screen :select-board)
    "3" (assoc state :players [:human :human] :screen :select-board)
    "4" (assoc state :players [:ai :ai] :screen :select-board)))

(defmethod handle-screen :select-board [state query]
  (case (get query "choice")
    "1" (assoc state :board-size :3x3 :board (board/get-board :3x3) :screen :select-difficulty)
    "2" (assoc state :board-size :4x4 :board (board/get-board :4x4) :screen :select-difficulty)
    "3" (assoc state :board-size :3x3x3 :board (board/get-board :3x3x3) :screen :select-difficulty)))

(defmethod handle-screen :select-difficulty [state query]
  (let [diff (case (get query "choice")
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

(defmethod handle-screen :game [state query]
  (let [marker (if (= (:turn state) "p1")
                 (first (:markers state))
                 (second (:markers state)))
        idx (Integer/parseInt (get query "choice"))
        updated-state (assoc state :board (assoc (:board state) idx [marker]) :turn (game/next-player (:turn state)))
        empty? (= "" (first (nth (:board state) idx)))]
    (if empty?
      (do
        (db/update-current-game! updated-state idx)
        (assoc state :board (assoc (:board state) idx [marker])
          :screen :game
          :turn (game/next-player (:turn state))))
      state)))

(defmethod handle-screen :game-over [state query]
  (let [winner (board/check-winner (:board state))]
    (prn "state -" state)
    (prn "winner -" winner)
    ))

(defmethod handle-screen :in-progress-game [state query]
  (let [game (db/in-progress? {:store (:store state)})]
    (case (get query "choice")
      "1" game
      "2" (if (db/previous-games? {:store (:store state)})
            (assoc state :screen :replay-confirm)
            (assoc state :screen :select-game-mode)))))

(defmethod handle-screen :replay-confirm [state query]
  (case (get query "choice")
    "1" (let [id (Integer/parseInt (get query "match-id"))]
          (if-let [game (db/find-game-by-id {:store (:store state)} id)]
            (assoc game :screen :replay :board (board/get-board (:board-size game)))
            state))
    (= "2" (get query "choice")) (assoc state :screen :select-game-mode)
    :else state))

(defmethod handle-screen :replay [state query]
  state)