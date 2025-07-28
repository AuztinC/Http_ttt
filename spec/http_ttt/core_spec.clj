(ns http-ttt.core-spec
  (:require [speclj.core :refer :all]
            [http-ttt.tttHandler :refer :all]
            [http-ttt.core :as sut])
  (:import (Server Server Sleep StatusCode)
           (Server.Routes RouteHandler)
           [http_ttt.tttHandler TttHandler]
           (Server.HTTP HttpResponse)))

; obj.someMethod(10)
; (.someMethod obj 10)
;
; "<h1>Tic Tac Toe</h1>".getBytes()
; arr
; Array.toString() => B[@0x9876
;
; Object a = new Object()
; (def a (Object.))
;

(defprotocol MyProtocol
  (foo [_this]))

(deftype MyType []
  MyProtocol
  (foo [_this] (prn "bar")))


(deftype MySleeper []
  Sleep
  (sleep [_this millis] (prn (format "slept %s millis" millis))))

(describe "main"
  (with-stubs)

  #_(it "constructs TttHandler with :mem store by default"
    ;; define a stubbed Server
    (let [fake-handler (reify RouteHandler
                         (handle [_ _]
                           (HttpResponse. StatusCode/OK "text/html" (.getBytes "<h1>"))))

          fake-server (stub [:server
                             :addRoute (fn [_ _ _] nil)
                             :start (fn [_] nil)])]

      (with-redefs [sut/handler-factory (fn [store]
                                          (should= :mem store)
                                          fake-handler)
                    sut/server-factory (fn [_] fake-server)]
        (sut/-main)

        ;; optionally assert that .addRoute was called
        (should-have-invoked :server {:with [:any "/ttt" fake-handler]}))))




  )
