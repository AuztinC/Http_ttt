(ns http-ttt.handler-spec
  (:require [speclj.core :refer :all]
            [http-ttt.tttHandler :as sut])
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

  (context "post"
    (it "select-game-mode goes to Select board"
      (should-contain "<h1>Select a board</h1>" (body Methods/POST "/ttt?choice=1")))

    (it "select-difficulty page after board selection"
      (should-contain "<h1>Select difficulty</h1>"
        (body Methods/POST "/ttt?screen=select-board&choice=1"))))

  (context "parse-query-params"
    (it "parses single param"
      (should= {"choice" "1"} (sut/parse-query-params "/ttt?choice=1")))

    (it "parses multiple params"
      (should= {"screen" "select-difficulty" "choice" "2"}
        (sut/parse-query-params "/ttt?screen=select-difficulty&choice=2")))

    (it "returns nil when no query"
      (should= nil (sut/parse-query-params "/ttt")))

    #_(it "builds game state from params"
      (with-redefs [http-ttt.render-screen/render-screen (stub :render-screen)]
       (let [out (body Methods/POST "/ttt?screen=select-difficulty&players=human-ai&board-size=3x3&choice=1")]
       (should= {"screen" "select-difficulty" "choice" "2"}
        out))))
    )
  )
