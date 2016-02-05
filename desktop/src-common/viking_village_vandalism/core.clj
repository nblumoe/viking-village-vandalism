(ns viking-village-vandalism.core
  (:require [play-clj.core :as play-clj]
            [play-clj.g2d :as g2d]
            [clojure.test :refer [is]]))

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
(def health-coords [(* 0.9 screen-width) (* 0.9 screen-height)])
(def initial-health 2)

(def floor-y (* 0.1 screen-height))
(def arrow-y (* screen-height 2))
(def obstacle-speed 20)
(def barrel-rotation-speed 10)

(def player-x 30)
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

(declare on-show! init-screen!
         on-render update-entities render-entities!
         on-timer
         on-key-down
         on-begin-contact)

;; the main game screen
(play-clj/defscreen main-screen
  :on-show on-show!                   ; Screen Entities -> Entities
  :on-render on-render                ; Screen Entities -> Entities
  :on-timer on-timer                  ; Screen Entities -> Entities
  :on-key-down on-key-down            ; Screen Entities -> Entities
  :on-begin-contact on-begin-contact) ; Screen Entities -> Entities

;; the UI overlay for the main game screen
;; !!!
(play-clj/defscreen ui-screen)

(play-clj/defgame viking-village-vandalism-game
  :on-create
  (fn [this]
    (play-clj/set-screen! this main-screen #_ui-screen)))

;; Screen Entities -> Entities
;; initialize screen rendering and create background and player entities
;; !!! this does not compile on initial CIDER connect, but can be used once started
#_(with-redefs [init-screen! (fn [_] :screen-stub)
                g2d/texture* (fn [filename] {:texture-loaded filename})]
    (let [entities    (on-show! nil nil)
          players     (filter (partial instance? Player) entities)
          obstacles   (filter :obstacle? entities)]
      (is (= (count players) 1)     "only a single player gets added")
      (is (= (count obstacles) 0)   "no obstacles created on start")
      (is (= (first players)
             (map->Player
              {:dy            0               ; starts on the floor
               :health        initial-health  ; initial health
               :score         0               ; initial score
               :current-image :running        ; start as running
               })) "player initialized with correct values")))

(defn on-show! [screen _]
  (init-screen! screen)
  [(g2d/texture background-image)
   (merge (g2d/texture (get player-images :running))
          {:player? true
           :images (reduce-kv #(assoc %1 %2 (g2d/texture %3)) {} player-images)}
          (map->Player {:dy            0
                        :health        initial-health
                        :score         0
                        :current-image :running}))])

;; Screen !-> Screen
;; initialize the screen
;; !!! not testing this for now, as side-effects of `update!` are a PITA
(defn init-screen! [screen]
  (-> screen
      (play-clj/update! :renderer (play-clj/stage))
      (play-clj/add-timer! :spawn-obstacle 5 1)))

;; Screen Entities -> Entities
;; update world state and render entities, produce updated entities
(defn on-render [screen entities]
  (->> entities
       update-entities
       (render-entities! screen)))

;; Entities -> Entities
;; produce an updated world state
;; !!!
(defn update-entities [entities]
  (->> entities
       (map (fn [{:keys [current-image] :as entity}]
              (cond (:player? entity)
                    (merge entity
                           (get-in entity [:images current-image]))

                    :else entity)))))

;; Entities !-> Entities
;; render entities, returns the original entities
(defn render-entities! [screen entities]
  (play-clj/clear!)
  (play-clj/render! screen entities))

;; Screen Entities -> Entities
;; produce updated world state on timed events
;; !!!
(defn on-timer [screen entities]
  entities)

;; Screen Entities -> Entities
;; produce updated world state on key inputs
;; !!!
(defn on-key-down [screen entities]
  entities)

;; Screen Entities -> Entities
;; produce updated world state on collision of entities
;; !!!
(defn on-begin-contact [screen entities]
  entities)

;; ===============================
;; Development time helpers:

(play-clj/defscreen blank-screen
  :on-render
  (fn [screen entities]
    (play-clj/clear!)))

;; fall back to blank screen on errors
(play-clj/set-screen-wrapper! (fn [screen screen-fn]
                                (try (screen-fn)
                                     (catch Exception e
                                       (.printStackTrace e)
                                       (play-clj/set-screen! viking-village-vandalism-game blank-screen)))))


;; use this to switch screens and retrigger on-show
(comment
  (play-clj/on-gl (play-clj/set-screen! viking-village-vandalism-game main-screen))

  (play-clj/on-gl (play-clj/set-screen! viking-village-vandalism-game blank-screen))

  )
