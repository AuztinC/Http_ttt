(ns http-ttt.tttHandler
  (:require [clojure.string :as str]
            [http-ttt.render-screen :refer [render-screen]]
            [http-ttt.transition :as transition]
            [tic-tac-toe.board :as board]
            [tic-tac-toe.game :as game]
            [tic-tac-toe.persistence :as db]
            [tic-tac-toe.replay :as replay])
  (:import (Server StatusCode)
           (Server.HTTP HttpRequest HttpResponse)
           (Server.Routes RouteHandler)))

(defn- determine-starting-screen [store query]
  (cond
    (db/in-progress? {:store store}) :in-progress-game
    (db/previous-games? {:store store}) :replay-confirm
    :else (keyword (get query "screen" "select-game-mode"))))

(defn parse-query-params [path]
  (when (str/includes? path "?")
    (let [[_ query-string] (str/split path #"\?" 2)]
      (->> (str/split query-string #"&")
        (map #(str/split % #"=" 2))
        (into {})))))

(defn parse-hyphens [param]
  (when param (vec (map keyword (str/split param #"-")))))

(defn- parse-cookies [^String cookie-header]
  (when cookie-header
    (->> (str/split cookie-header #";")
      (map str/trim)
      (map #(str/split % #"=" 2))
      (into {}))))

(defn query-state [query store]
  {:store        store
   :ui           :web
   :screen       (determine-starting-screen store query)
   :players      (parse-hyphens (get query "players"))
   :board-size   (keyword (get query "board-size"))
   :difficulties (parse-hyphens (get query "difficulties"))
   :turn         "p1"
   :markers      ["X" "O"]})

(defn retrieve-state [cookie-map store query]
  (let [full-state-cookie (when-let [str-game (get cookie-map "game")]
                            (let [game (read-string str-game)]
                              (cond
                                (not (= store (:store game)))
                                nil

                                (and (nil? query) (:board game))
                                (assoc game :screen :in-progress-game)

                                :else game)))]
    (or full-state-cookie
      (query-state query store))))

(defn handle-choice [state query]
  (if (get query "choice")
    (transition/handle-screen state query)
    state))

(defn auto-advance [next-state]
  (let [next-player (case (:turn next-state)
                      "p1" (first (:players next-state))
                      "p2" (second (:players next-state)))]
    (cond
      (= :game (:screen next-state))
      (if (= :ai next-player)
        (game/next-state next-state)
        next-state)

      (= :replay (:screen next-state))
      (replay/apply-next-replay-move next-state)

      :else next-state)))

(defn handle-request
  [{:keys [store path cookies]}]
  (let [query (parse-query-params path)
        cookie-map (parse-cookies cookies)
        state (retrieve-state cookie-map store query)
        next-state (handle-choice state query)
        auto-advance (auto-advance next-state)
        final-state (if (and (= :game (:screen auto-advance))
                          (:board auto-advance) (board/check-winner (:board auto-advance)))
                      (assoc auto-advance :screen :game-over)
                      auto-advance)
        html (render-screen final-state)]
    {:state final-state
     :html  html}))

(deftype TttHandler [store]
  RouteHandler
  (handle [_ req]
    (let [path (String. (.getPath req))
          cookies (try (.getHeader req "cookie") (catch Exception _ nil))
          {:keys [state html]} (handle-request {:store store :path path :cookies cookies})
          body (byte-array (.getBytes html))
          header (cond
                   (= :game-over (:screen state))
                   (byte-array (.getBytes (str "Set-Cookie: game=; Max-Age=0; Path=/\r\n" "Content-Type: text/html\r\n")))
                   :else (byte-array (.getBytes (str "Set-Cookie: game=" state "; Path=/\r\nContent-Type: text/html\r\n"))))
          statusOK (. StatusCode valueOf "OK")]
      (HttpResponse. statusOK header body))))
