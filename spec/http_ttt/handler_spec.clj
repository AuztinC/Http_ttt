(ns http-ttt.handler-spec
  (:require [speclj.core :refer :all]
            [http-ttt.tttHandler :as sut]
            [tic-tac-toe.board :as board]
            [tic-tac-toe.persistence :as db]
            [http-ttt.transition :as transition]
            [tic-tac-toe.replay :as replay])
  (:import (Server Methods StatusCode)
           (Server.HTTP HttpResponse HttpRequest)
           (http_ttt.tttHandler TttHandler)))

(defn body [method path] (let [^HttpRequest req (HttpRequest. method path "1.1" nil nil)
                               handler (TttHandler. :mem)
                               ^HttpResponse response (.handle handler req)
                               body (String. (.getBody response))]
                           body))

(def dummy-cookie-game (pr-str {:id 5 :players [:ai :ai] :store :mem}))
(def dummy-query-game {:id 100 :screen :select-game-mode})

(describe "Handler"
  (with-stubs)

  (describe "sut/retrieve-state"

    (with-stubs)

    (redefs-around [db/find-game-by-id (stub :find-game-by-id)
                    sut/query-state (stub :query-state)])


    (it "returns full cookie state with :in-progress-game screen if query is nil"
      (let [cookie {"game" dummy-cookie-game}]
        (should= {:id 5 :players [:ai :ai] :screen :in-progress-game :store :mem}
          (sut/retrieve-state cookie :mem nil))))

    (it "returns full cookie state directly if query exists"
      (let [cookie {"game" dummy-cookie-game}]
        (should= {:id 5 :players [:ai :ai] :store :mem}
          (sut/retrieve-state cookie :mem {"choice" "1"}))))

    (it "falls back to query-state if nothing in cookies"
      (with-redefs [sut/query-state (fn [_ _] dummy-query-game)]
        (should= dummy-query-game
          (sut/retrieve-state {} :mem {"choice" "1"}))))

    (it "ignores blank gameId"
      (with-redefs [sut/query-state (fn [_ _] dummy-query-game)]
        (should= dummy-query-game
          (sut/retrieve-state {"gameId" ""} :mem {"choice" "1"}))))

    (it "handles missing cookies and nil query"
      (with-redefs [sut/query-state (fn [_ _] nil)]
        (should-be-nil (sut/retrieve-state {} :mem nil)))))


  (context "get"
    ;HttpRequest req = new HttpRequest(Methods.GET, "/");
    (it "select-game-mode"
      (should-contain "<h1>Select a game mode</h1>" (body Methods/GET "/ttt?screen=select-game-mode")))

    (it "display :in-progress"
        (let [state {:screen     :game
                     :store      :mem
                     :players    [:human :human]
                     :board      [["X"] [""] ["X"]
                                  [""] [""] [""]
                                  [""] [""] [""]]
                     :turn       "p1"
                     :markers    ["X" "O"]
                     :board-size :3x3}
              fake-cookie-header (str "game=" state)]
          (with-redefs [db/find-game-by-id (stub :find-game-by-id {:return state})]
            (let [{:keys [state]} (sut/handle-request {:store   :mem
                                                       :path    "/ttt"
                                                       :cookies fake-cookie-header})]
              (should= :in-progress-game (:screen state))))))
    )

  (it "winning board moves to :game-over"
    (let [winning-state {:screen     :game
                         :store      :mem
                         :players    [:human :human]
                         :board      [["X"] ["X"] ["X"]
                                      [""] [""] [""]
                                      [""] [""] [""]]
                         :turn       "p1"
                         :markers    ["X" "O"]
                         :board-size :3x3}
          fake-cookie-header (str "game=" winning-state)]

      (with-redefs [db/find-game-by-id (stub :find-game-by-id {:return winning-state})]
        (let [{:keys [state]} (sut/handle-request {:store   :mem
                                                   :path    "/ttt?screen=game&choice=2"
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

  (it "preserves players and board-size after choosing first move"
    (let [query "/ttt?screen=game&choice=0&players=human-ai&board-size=3x3&difficulties=hard"
          {:keys [state html]} (sut/handle-request
                                 {:store   :mem
                                  :path    query
                                  :cookies (str "game=" (pr-str {:screen     :game
                                                                 :store      :mem
                                                                 :players    [:human :human]
                                                                 :board      [["X"] [""] [""]
                                                                              [""] [""] [""]
                                                                              [""] [""] [""]]
                                                                 :turn       "p1"
                                                                 :board-size :3x3
                                                                 :markers    ["X" "O"]}))})]
      (should= [:human :human] (:players state))
      (should= :3x3 (:board-size state))
      (should= :game (:screen state))
      (should= [["X"] [""] [""] [""] [""] [""] [""] [""] [""]] (:board state))
      (should-contain "Tic-Tac-Toe!" html)
      (should-not-contain "Select a game mode" html)))

  (it "calls game/next-state when turn is AI"
    (let [initial-state {:screen     :game
                         :store      :mem
                         :players    [:ai :ai]
                         :board      [[""] [""] [""] [""] [""] [""] [""] [""] [""]]
                         :board-size :3x3
                         :turn       "p2"
                         :markers    ["X" "O"]
                         :id         0}]
      (with-redefs [tic-tac-toe.game/next-state (stub :next-state {:return initial-state})]
        (should-have-invoked :next-state (sut/auto-advance initial-state)))))

  (it "query state calls determine-start with store"
    (with-redefs [db/in-progress? (stub :in-progress? {:return {}})]
      (should= :in-progress-game (:screen (sut/query-state "/ttt" :mem)))))

  (it "calls apply-next-move for replay"
    (let [initial-state {:screen     :replay
                         :store      :mem
                         :board-size :3x3
                         :players    [:human :ai]
                         :turn       "p1"
                         :moves      [{:player "X", :position 0}
                                      {:player "O", :position 6}
                                      {:player "X", :position 1}
                                      {:player "O", :position 7}
                                      {:player "X", :position 2}]}
          game-str (pr-str initial-state)]
      (with-redefs [http-ttt.tttHandler/parse-cookies (constantly {"game" game-str})
                    tic-tac-toe.persistence/find-game-by-id (constantly nil)
                    tic-tac-toe.board/check-winner (constantly false)
                    tic-tac-toe.game/next-state (stub :next-state {:return initial-state})
                    replay/apply-next-replay-move (stub :next-move)
                    sut/retrieve-state (stub :retrieve-state {:return initial-state})
                    http-ttt.render-screen/render-screen (stub :render-screen)]
        (sut/handle-request {:store :mem :path "/ttt?screen=replay" :cookies (str "game=" game-str)})
        (should-have-invoked :next-move))))

  )
