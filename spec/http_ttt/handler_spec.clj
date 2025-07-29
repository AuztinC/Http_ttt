(ns http-ttt.handler-spec
  (:require [speclj.core :refer :all]
            [http-ttt.tttHandler :as sut]
            [tic-tac-toe.board :as board]
            [tic-tac-toe.persistence :as db])
  (:import (Server Methods StatusCode)
           (Server.HTTP HttpResponse HttpRequest)
           (http_ttt.tttHandler TttHandler)))

(defn body [method path] (let [^HttpRequest req (HttpRequest. method path "1.1" nil nil)
                               handler (TttHandler. :mem)
                               ^HttpResponse response (.handle handler req)
                               body (String. (.getBody response))]
                           body))

(describe "Handler"
  (with-stubs)
  (context "get"
    ;HttpRequest req = new HttpRequest(Methods.GET, "/");
    (it "select-game-mode"
      (should-contain "<h1>Select a game mode</h1>" (body Methods/GET "/"))))

  (it "winning board moves to :game-over"
    (let [winning-state {:screen :game
                         :store :mem
                         :players [:human :ai]
                         :board [["X"] ["X"] ["X"]
                                 [""]  [""] [""]
                                 [""]  [""] [""]]
                         :turn "p1"
                         :markers ["X" "O"]}
          fake-cookie-header (str "game=" winning-state)]

      (with-redefs [db/find-game-by-id (stub :find-game-by-id {:return winning-state})]
        (let [{:keys [state]} (sut/handle-request {:store :mem
                                                   :path "/ttt?screen=game&choice=2"
                                                   :cookies fake-cookie-header})]
          (should= :game-over (:screen state))))))

  (context "parse-query-params"
    (it "parses single param"
      (should= {"choice" "1"} (sut/parse-query-params "/ttt?choice=1")))

    (it "parses multiple params"
      (should= {"screen" "select-difficulty" "choice" "2"}
        (sut/parse-query-params "/ttt?screen=select-difficulty&choice=2")))

    (it "returns nil when no query"
      (should= nil (sut/parse-query-params "/ttt")))
    )

  (it "returns true new game"
    (let [state {:screen :game
                 :board (board/get-board :3x3)
                 :board-size :3x3}]
      (should (sut/new-game? state))))
  (it "returns false game in progress"
    (let [state {:screen :game
                 :board [["X"] [""] [""] [""] [""] [""] [""] [""] [""]]
                 :board-size :3x3}]
      (should-not (sut/new-game? state))))

  #_(focus-it "preserves players and board-size after choosing first move"
    (let [query "/ttt?screen=game&choice=0&players=human-ai&board-size=4x4&difficulties=hard"
          {:keys [state html]} (sut/handle-request {:store :mem :path query :cookies nil})]
      (should= [:human :ai] (:players state))
      (should= :4x4 (:board-size state))
      (should= :game (:screen state))
      (should= [["X"] [""] [""] [""] [""] [""] [""] [""] [""]] (:board state))
      (should-contain "Ur gamin" html)
      (should-not-contain "Select a game mode" html)))

  )
