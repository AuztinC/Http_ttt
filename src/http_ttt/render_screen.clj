(ns http-ttt.render-screen
  (:require [clojure.string :as str]
            [hiccup2.core :as h]
            [tic-tac-toe.board :as board]))

(defn auto-refresh? [state]
  (and
    (= :game (:screen state))
    (= [:ai :ai] (:players state))
    (not (board/check-winner (:board state)))))

(defn hidden-fields [state]
  (filter some?
    [[:input {:type "hidden" :name "screen" :value (name (:screen state))}]
     (when-let [players (:players state)]
       [:input {:type "hidden" :name "players" :value (str/join "-" (map name players))}])
     (when-let [diffs (:difficulties state)]
       [:input {:type "hidden" :name "difficulties" :value (str/join "-" (map name diffs))}])
     (when-let [board (:board-size state)]
       [:input {:type "hidden" :name "board-size" :value (str (name board))}])]))

(defmulti render-screen :screen)

(defmethod render-screen :select-game-mode [state]
  (-> [:html
       [:head [:title "Tic Tac Toe"]]
       [:body
        [:h1 "Select a game mode"]
        (apply vector :form {:method "get" :action "/ttt"}
          (concat (hidden-fields state)
            [[:button {:type "submit" :name "choice" :value "1"} "Human vs AI"]
             [:button {:type "submit" :name "choice" :value "2"} "AI vs Human"]
             [:button {:type "submit" :name "choice" :value "3"} "Human vs Human"]
             [:button {:type "submit" :name "choice" :value "4"} "AI vs AI"]]))]]
    h/html
    str))

(defmethod render-screen :select-board [state]
  (-> [:html
       [:head [:title "Tic Tac Toe"]]
       [:body
        [:h1 "Select a board"]
        (apply vector :form {:method "get" :action "/ttt"}
          (concat (hidden-fields state)
            [[:button {:type "submit" :name "choice" :value "1"} "3x3"]
             [:button {:type "submit" :name "choice" :value "2"} "4x4"]
             [:button {:type "submit" :name "choice" :value "3"} "3x3x3"]]))]]
    h/html
    str))

(defmethod render-screen :select-difficulty [state]
  (-> [:html
       [:head [:title "Tic Tac Toe"]]
       [:body
        [:h1 "Select difficulty"]
        (apply vector :form {:method "get" :action "/ttt"}
          (concat (hidden-fields state)
            [[:button {:type "submit" :name "choice" :value "1"} "Easy"]
             [:button {:type "submit" :name "choice" :value "2"} "Medium"]
             [:button {:type "submit" :name "choice" :value "3"} "Hard"]]))]]
    h/html
    str))

(defn render-cell [idx value state]
  (if (empty? value)
    [:td {:style {:width "60px" :height "60px" :border "1px solid black"}}
     [:form {:method "get" :action "/ttt"}
      [:input {:type "hidden" :name "choice" :value idx}]
      [:input {:type "hidden" :name "screen" :value "game"}]
      [:input {:type "hidden" :name "players" :value (str/join "-" (map name (:players state)))}]
      [:input {:type "hidden" :name "board-size" :value (name (:board-size state))}]
      [:input {:type "hidden" :name "difficulties" :value (str/join "-" (map name (:difficulties state)))}]
      [:button {:type "submit" :style {:background-color "white" :width "60px" :height "60px" :border "none"}} idx]]
     ]
    [:td {:style {:width "60px" :height "60px" :text-align "center" :font-size "2em" :border "1px solid black"}}
     value]))

(defn render-board [board size state]
  (let [dim (case size
              :3x3 3
              :4x4 4
              :3x3x3 9)]
    (->> (map-indexed vector board)
      (partition dim)
      (map (fn [row] [:tr (map (fn [[idx cell]]
                                 (render-cell idx (first cell) state))
                            row)]))
      (into [:table]))))

(defmethod render-screen :game [state]
  (-> [:html
       [:head [:title "Tic Tac Toe"]
        (when (auto-refresh? state)
          [:meta {:http-equiv "refresh" :content "1"}])]
       [:body
        [:h1 "Tic-Tac-Toe!"]
        (render-board (:board state) (:board-size state) state)]]
    h/html
    str))

(defmethod render-screen :game-over [state]
  (let [winner (board/check-winner (:board state))]
    (-> [:html
         [:head [:title "Tic Tac Toe"]]
         [:body
          [:h1 "Game Over"]
          (if (= "tie" winner)
            [:h3 (str "Tie Game!" )]
            [:h3 (str "Winner is " winner)])
          (render-board (:board state) (:board-size state) state)
          [:form {:method "get" :action "/ttt"}
           [:button {:type "submit" :name "/" :value ""} "New Game?"]]]]
      h/html
      str)))