(ns viking-village-vandalism.core
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]))

;; ===============================
;; Constants:
(def screen-width  800)
(def screen-height 600)
(def background-image "background.png")
(def player-images {:running "player-run.png"
                    :jumping "player-jump.png"
                    :sliding "player-sliding.png"})
(def barrel-image "barrel.png")
(def arrow-image  "arrow.png")
(def gate-images {:intact "gate-intact.ong"
                  :broken "gate-broken.png"})

(def score-coords [(* 0.1 screen-width) (* 0.9 screen-height)])
(def health-image "health.png")
(def health-coors [(* 0.9 screen-width) (* 0.9 screen-height)])

(def floor-y (* 0.1 screen-height))
(def arrow-y (* screen-height 2))
(def obstacle-speed 20)
(def barrel-rotation-speed 10)

(def player-kick-duration 1)   ; in seconds
(def player-slide-duration 1)  ; in seconds
(def player-speed 10)
(def player-jump-height (* arrow-y 1.5))

;; ===============================
;; Data Definitions:

(defrecord Player [current-image dy health score])
;; Player is (map->Player {:current-image Keyword :dy Number[0, screen-height] :health Natural :score Natural)
;; interp. the player character with an image identifier for the current player action,
;;         the y offset from the floor, current health and score
(map->Player {:current-image :running :dy  0 :health 3 :score 5})   ; just running
(map->Player {:current-image :jumping :dy 57 :health 3 :score 15})  ; jumping
(map->Player {:current-image :running :dy  0 :health 0 :score 15})  ; dead

(defn fn-for-player [{:keys [current-image dy health score] :as player}]
  (... current-image dy health score))

(defrecord Obstacle [x rot type])
;; Obstacle is (Obstacle. Number[0, screen-width] Number Keyword)
;; interp. a barrel, arrow or gate hurting the player on collision
;;         - x is the position in screen coordinates along the x axis
;;         - rot is the rotation in radians (only for barrels)
;;         - type is one of: [:barrel :arrow :gate]
(map->Obstacle {:x 123 :rot (* 0.75 Math/PI) :type :barrel}) ; a rolling barrel
(map->Obstacle {:x   0 :rot 0 :type :arrow})                 ; an arrow about to leave the scene
(map->Obstacle {:x 342 :rot 0 :type :gate})                  ; a gate

(defn fn-for-obstacle [{:keys [x rot type] :as obstacle}]
  (... x rot type))

;; ===============================
;; Function Definitions:

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    (label "Hello world!" (color :white)))

  :on-render
  (fn [screen entities]
    (clear!)
    (render! screen entities)))

(defgame viking-village-vandalism-game
  :on-create
  (fn [this]
    (set-screen! this main-screen)))
