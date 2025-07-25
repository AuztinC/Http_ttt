(ns http-ttt.core-spec
  (:require [speclj.core :refer :all]
            [http-ttt.core :refer :all])
  (:import (Server Sleep)))

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

(describe "a test"
  (it "equals bytes"
    (should= "<h1>Tic Tac Toe</h1>"
      (String. (.getBytes "<h1>Tic Tac Toe</h1>"))))

  (it "foos"
    (should= nil (foo (MyType.)))
    (should= 1 (.sleep (MySleeper.) 10))))
