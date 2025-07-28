(ns http-ttt.handler-spec
  (:require [speclj.core :refer :all]
            [http-ttt.tttHandler :as sut])
  (:import (Server Methods StatusCode)
           (Server.HTTP HttpResponse HttpRequest)
           (http_ttt.tttHandler TttHandler)))

(describe "Handler"

  (context "get"
    ;HttpRequest req = new HttpRequest(Methods.GET, "/");
    (it "select-game-mode"
      (let [^HttpRequest req (HttpRequest. Methods/GET "/" "1.1" nil nil)
            handler (TttHandler. :mem)
            ^HttpResponse response (.handle handler req)
            body (String. (.getBody response))]
        (should-contain "<h1>Select a game mode</h1>" body))))

  (context "post"
    (it "select-game-mode goes to Select board"
      (let [^HttpRequest req (HttpRequest. Methods/POST "/ttt?choice=1")
            handler (TttHandler. :mem)
            ^HttpResponse response (.handle handler req)
            body (String. (.getBody response))]
        (should-contain "<h1>Select a board</h1>" body))))

  )
