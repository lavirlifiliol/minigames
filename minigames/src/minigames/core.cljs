(ns minigames.core
    (:require
      [reagent.core :as r]
      [reagent.dom :as d]
      [clojure.edn :as edn]))

;; -------------------------
;; Views

(def phase (r/atom :wait)) ; wait show guess
(def won (r/atom false))
(def side (r/atom "6"))
(def ratio (r/atom "3"))
(def grid-state (r/atom #{})) 
(def correct (r/atom #{}))


(defn grid-click [x y]
  (fn []
    (and (= @phase :guess)
      (if (@correct [x y])
        (do
          (swap! grid-state #(conj % [x y]))
          (and
            (= @grid-state @correct)
            (do
              (reset! phase :wait)
              (reset! won true))))
        (do
          (reset! grid-state @correct)
          (reset! phase :wait))))))
(defn read-number-or-default [s d]
  (let
    [n (try
         (edn/read-string s)
         (catch js/Object a d))]
    (if (number? n) n d)))

(defn compute-side []
  (min 99 (read-number-or-default @side 6)))

(defn play []
  (and (= @phase :wait) (do
    (js/console.log "playing!")
    (reset!
      grid-state
      (->>
        (for [x (range (compute-side)) y (range (compute-side))] [x y])
        (shuffle)
        (take (/ (* (compute-side) (compute-side)) (read-number-or-default @ratio 3)))
        (set)))
    (reset! phase :show)
    (reset! won false)
    (js/setTimeout
      (fn []
        (reset! correct @grid-state)
        (reset! phase :guess)
        (reset! grid-state #{}))
      2000))))

(defn onkey [ev]
  (and (#{"Enter" " "} ev.key)
    (play)))

(defn row [w y]
  [:div {:key y}
   (doall (for [x (range w)]
     [:div
      {:key x
       :class (if (@grid-state [x y]) "sel" (if (= @phase :guess) "unsel"))
       :on-mouse-down (grid-click x y)}]))])

(defn grid [w h]
  [:div {:class "grid"}
   (doall (for [y (range h)]
    (row w y)))])


(defn atom-input [id at]
  [:input {:type "text"
           :id id
           :value @at
           :on-change #(reset! at (-> % .-target .-value ))}])

(defn config [] 
  [:div {:class "cfg"}
   [:label {:for "side"} "side"]
   [atom-input "side" side]
   [:label {:for "part"} "part"]
   [atom-input "part" ratio] ])

(defn home-page []
  [:div
   [:div {:class (if (= @phase :wait) "" "hide")} [config]]
   [grid (compute-side)(compute-side)]
   (and
     (= @phase :wait)
     (if (empty? @correct)
       [:h2 {:class "msg"} "Press space to play"]
       (if @won
         [:h2 {:class  "msg won" } "Correct, press space to play again"]
         [:h2 {:class  "msg lost" } "Incorrect, press space to try again"])))])

;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root)
  (js/window.addEventListener "keydown" onkey))
