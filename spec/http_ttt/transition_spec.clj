(ns http-ttt.transition-spec
  (:require [speclj.core :refer :all]
            [http-ttt.transition :as sut]
            [http-ttt.tttHandler :refer :all])
  (:import (Server.HTTP HttpResponse)
           (http_ttt.tttHandler TttHandler)))


(describe "transitions"

  (it "select-game-mode goes to select-board"
    (let [state {:screen :select-game-mode}
          choice "1"
          response (sut/transition state choice)]
      (should= :select-board (:screen response))))

  )
