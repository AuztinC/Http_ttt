(ns http-ttt.main-spec
  (:require [speclj.core :refer :all]
            [http-ttt.main :as sut]
            [tic-tac-toe.psql :as pg])
  (:import (Server ServerArgs)
           (Server.Routes RouteHandler)
           (Server.HTTP HttpResponse)
           (Server Server StatusCode)))

(defprotocol DummyServer
  (addRoute [_this ^String path handler])
  (start [_this]))

(defn dummy-server [captured]
  (reify DummyServer
    (addRoute [_this path handler]
      (swap! captured assoc :route [path handler]))
    (start [_this]
      (swap! captured assoc :started true))))

(def dummy-handler
  (reify RouteHandler
    (handle [_ _]
      (HttpResponse. StatusCode/OK
        (.getBytes "Content-Type: text/html")
        (.getBytes "<h1>Hello</h1>")))))


(describe "-main"
  (with-stubs)
  (it "constructs TttHandler with :mem store and adds route"
    (let [captured (atom {})
          fake-handler dummy-handler
          fake-server (dummy-server captured)]
      (with-redefs [sut/handler-factory (fn [store]
                                          (should= :mem store)
                                          fake-handler)
                    sut/server-factory (fn [_] fake-server)]
        (sut/-main)
        (should= ["/ttt\\?*.*" fake-handler] (:route @captured))
        (should (:started @captured)))))

  (it "constructs TttHandler with :file store and adds route"
    (let [captured (atom {})
          fake-handler dummy-handler
          fake-server (dummy-server captured)]
      (with-redefs [sut/handler-factory (fn [store]
                                          (should= :file store)
                                          fake-handler)
                    sut/server-factory (fn [_] fake-server)]
        (sut/-main "-file")
        (should= ["/ttt\\?*.*" fake-handler] (:route @captured))
        (should (:started @captured)))))

  (it "constructs TttHandler with :psql store and adds route"
    (let [captured (atom {})
          fake-handler dummy-handler
          fake-server (dummy-server captured)]
      (with-redefs [sut/handler-factory (fn [store]
                                          (should= :psql store)
                                          fake-handler)
                    sut/server-factory (fn [_] fake-server)
                    pg/db-setup (stub :db-setup)]
        (sut/-main "-psql")
        (should= ["/ttt\\?*.*" fake-handler] (:route @captured))
        (should (:started @captured)))))

  )

