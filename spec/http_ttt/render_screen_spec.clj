(ns http-ttt.render-screen-spec
  (:require [speclj.core :refer :all]
            [http-ttt.render-screen :as sut]))

(describe "render screen"

  (it "select-board-size"
    (let [state {:screen :select-board}
          response (sut/render-screen state)]
      (should-contain "<h1>Select a board</h1>"
        response)))
  )
