(ns http-ttt.render-screen
  (:require [hiccup2.core :as h]))


(defmulti render-screen :screen)

(defmethod render-screen :select-game-mode [_state]
  (-> [:html
       [:head [:title "Tic Tac Toe"]]
       [:body
        [:h1 "Select a game mode"]
        [:form {:method "get" :action "/ttt"}
         [:button {:type "submit" :name "choice" :value "1"} "1: Human vs AI"]
         [:button {:type "submit" :name "choice" :value "2"} "2: AI vs Human"]
         [:button {:type "submit" :name "choice" :value "3"} "3: Human vs Human"]
         [:button {:type "submit" :name "choice" :value "4"} "3: AI vs AI"]]]]
    h/html
    str))

(defmethod render-screen :select-board [_state]
  (-> [:html
       [:head [:title "Tic Tac Toe"]]
       [:body
        [:h1 "Select a board"]
        [:form {:method "get" :action "/ttt"}
         [:button {:type "submit" :name "choice" :value "1"} "1: 3x3"]
         [:button {:type "submit" :name "choice" :value "2"} "2: 4x4"]
         [:button {:type "submit" :name "choice" :value "3"} "3: 3x3x3"]]]]
    h/html
    str))