(ns http-ttt.render-screen-spec
  (:require [speclj.core :refer :all]
            [http-ttt.render-screen :as sut]))

(describe "render screen"

  (it "select-board-size"
    (let [state {:screen :select-board}
          response (sut/render-screen state)]
      (should-contain "<h1>Select a board</h1>"
        response)))

  (it "select-difficulty"
    (let [state {:screen :select-difficulty}
          response (sut/render-screen state)]
      (should-contain "<h1>Select difficulty</h1>"
        response)))
  )
