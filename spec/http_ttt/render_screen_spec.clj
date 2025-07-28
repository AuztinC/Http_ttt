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

  (it "hidden fields"
    (let [state {:screen       :game
                 :players      [:ai :human]
                 :board-size   :3x3
                 :difficulties [:easy :hard]}
          output (sut/hidden-fields state)]
      (should= [[:input {:type "hidden", :name "screen", :value "game"}]
                [:input {:type "hidden", :name "players", :value "ai-human"}]
                [:input {:type "hidden", :name "difficulties", :value "easy-hard"}]
                [:input {:type "hidden", :name "board-size", :value "3x3"}]]
        output)))
  )
