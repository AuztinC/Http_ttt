(ns http-ttt.transition-spec
  (:require [speclj.core :refer :all]
            [http-ttt.transition :as sut]
            [http-ttt.tttHandler :refer :all])
  (:import (Server.HTTP HttpResponse)
           (http_ttt.tttHandler TttHandler)))


(describe "transitions"

  (it "game-mode goes to select-board"
    (let [state {:screen :select-game-mode}
          choice "1"
          response (sut/transition state choice)]
      (should= :select-board (:screen response))
      (should= [:human  :ai] (:players response))))

  (it "board goes to difficulty"
    (let [state {:screen :select-board}
          choice "1"
          response (sut/transition state choice)]
      (should= :select-difficulty (:screen response))
      (should= :3x3 (:board-size response))))

  (it "difficulty goes to game"
    (let [state {:screen :select-difficulty :store :mem}
          choice "1"
          response (sut/transition state choice)]
      (should= :game (:screen response))
      (should= [:easy] (:difficulties response))))

  (it "2 difficulties for [ai ai] goes to game"
    (let [state {:screen :select-difficulty :store :mem :players [:ai :ai]}
          choice "1"
          response (sut/transition state choice)]
      (should= :select-difficulty (:screen response))
      (should= [:easy] (:difficulties response))))

  )
