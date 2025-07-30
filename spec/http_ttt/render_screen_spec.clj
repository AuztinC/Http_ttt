(ns http-ttt.render-screen-spec
  (:require [speclj.core :refer :all]
            [http-ttt.render-screen :as sut]
            [tic-tac-toe.board :as board]
            [tic-tac-toe.persistence :as db]))

(def fake-board (board/get-board :3x3))

(describe "render screen"
  (with-stubs)
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

  (it "displays in-progress game"
    (let [state {:screen :in-progress-game}
          output (sut/render-screen state)]
      (should-contain "<h1>Previous game detected! Resume?</h1>"
        output)))

  (it "display replay-confirm"
    (with-redefs [db/previous-games? (stub :previous-games)]
      (let [state {:screen :replay-confirm}
            output (sut/render-screen state)]
        (should-contain "<h1>Would you like to watch a replay?</h1>"
          output))))

  (it "display replay game"
    (let [state {:screen :replay
                 :id 1
                 :board-size :3x3}
          output (sut/render-screen state)]
      (should-contain "<h1>Replaying game 1</h1>"
        output)))

  (it "game over shows winner"
    (with-redefs [sut/render-board (stub :render-board)]
      (let [state {:screen :game-over
                   :board (repeat 9 ["X"])
                   :board-size :3x3}
            response (sut/render-screen state)]
        (should-contain "Winner is X" response))))

  (it "in game"
    (with-redefs [sut/render-board (stub :render-board)]
      (let [state {:screen :game}
            response (sut/render-screen state)]
        (should-contain "<h1>Tic-Tac-Toe!</h1>"
          response))))

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

  (it "adds meta tag for ai ai"
    (let [state {:screen     :game
                 :players    [:ai :ai]
                 :board-size :3x3
                 :board      fake-board
                 :turn       "p1"
                 :markers    ["X" "O"]}
          output (sut/render-screen state)]
      (should-contain "http-equiv=\"refresh\"" output)))


  (context "auto-refresh?"
    (it "returns true for :game screen with [:ai :ai]"
      (should (sut/auto-refresh? {:screen :game :players [:ai :ai]})))

    (it "returns true for :replay screen"
      (should (sut/auto-refresh? {:screen :replay})))

    (it "returns false for :game screen with [:human :ai]"
      (should-not (sut/auto-refresh? {:screen :game :players [:human :ai]})))

    (it "returns false for :select-game-mode"
      (should-not (sut/auto-refresh? {:screen :select-game-mode}))))



  )
