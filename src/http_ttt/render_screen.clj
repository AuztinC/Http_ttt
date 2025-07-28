(ns http-ttt.render-screen
  (:require [hiccup2.core :as h]))


(defmulti render-screen :screen)

(defmethod render-screen :select-game-mode [_state]
  (-> [:html
       [:head [:title "Tic Tac Toe"]]
       [:body
        [:h1 "Select a game mode"]
        [:form {:method "get" :action "/ttt"}
         [:input {:type "hidden" :name "screen" :value "select-game-mode"}]
         [:button {:type "submit" :name "choice" :value "1"} "Human vs AI"]
         [:button {:type "submit" :name "choice" :value "2"} "AI vs Human"]
         [:button {:type "submit" :name "choice" :value "3"} "Human vs Human"]
         [:button {:type "submit" :name "choice" :value "4"} "AI vs AI"]]]]
    h/html
    str))

(defmethod render-screen :select-board [_state]
  (-> [:html
       [:head [:title "Tic Tac Toe"]]
       [:body
        [:h1 "Select a board"]
        [:form {:method "get" :action "/ttt"}
         [:input {:type "hidden" :name "screen" :value "select-board"}]
         [:button {:type "submit" :name "choice" :value "1"} "3x3"]
         [:button {:type "submit" :name "choice" :value "2"} "4x4"]
         [:button {:type "submit" :name "choice" :value "3"} "3x3x3"]]]]
    h/html
    str))

(defmethod render-screen :select-difficulty [_state]
  (-> [:html
       [:head [:title "Tic Tac Toe"]]
       [:body
        [:h1 "Select difficulty"]
        [:form {:method "get" :action "/ttt"}
         [:input {:type "hidden" :name "screen" :value "select-difficulty"}]
         [:button {:type "submit" :name "choice" :value "1"} "Easy"]
         [:button {:type "submit" :name "choice" :value "2"} "Medium"]
         [:button {:type "submit" :name "choice" :value "3"} "Hard"]]]]
    h/html
    str))