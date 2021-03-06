(ns viking-village-vandalism.core
  (:require [play-clj.core :as play-clj]
            [play-clj.g2d :as g2d]
            [play-clj.ui :as ui]
            [clojure.test :refer [is]]))

;; ===============================
;; Constants:
(def game-title "Viking Village Vandalism")
(def screen-width  800)
(def screen-height 600)
(def background-image "background.png")
(def player-animations {:running (map #(str "shieldmaiden/5x/run_" % ".png") (range 6))
                        :jumping (map #(str "shieldmaiden/5x/jump_" % ".png") (range 6))})
(def barrel-animation (map #(format "obstacles/barrel/barrel_%03d.png" %) (range 11)))

(def arrow-image  "arrow.png")
(def gate-images {:intact "gate-intact.ong"
                  :broken "gate-broken.png"})

(def score-coords [(* 0.1 screen-width) (* 0.9 screen-height)])
(def health-image "health.png")
(def health-coords [(* 0.9 screen-width) (* 0.9 screen-height)])
(def initial-health 2)

(def floor-y (* 0.05 screen-height))
(def arrow-y (* screen-height 2))
(def obstacle-speed 5)
(def obstacle-interval 1)
(def obstacle-destroy-x -100)

(def player-x 50)
(def player-kick-duration 1)   ; in seconds
(def player-slide-duration 1)  ; in seconds
(def player-speed 5)
(def player-max-y-velocity 150)
(def gravity -0.5)

;; ===============================
;; Data Definitions:

(defrecord Player [current-animation health score])
;; Player is (map->Player {:current-animation Keyword :health Natural :score Natural)
;; interp. the player character with an image identifier for the current player animation,
;;         current health and score
(map->Player {:current-animation :running :health 3 :score 5})   ; just running
(map->Player {:current-animation :jumping :health 3 :score 15})  ; jumping
(map->Player {:current-animation :running :health 0 :score 15})  ; dead

(defn fn-for-player [{:keys [current-animation health score] :as player}]
  (... current-animation health score))

(defrecord Obstacle [x type])
;; Obstacle is (Obstacle. Number[0, screen-width] Number Keyword)
;; interp. a barrel, arrow or gate hurting the player on collision
;;         - x is the position in screen coordinates along the x axis
;;         - type is one of: [:barrel :arrow :gate]
(map->Obstacle {:x 123 :type :barrel}) ; a rolling barrel
(map->Obstacle {:x   0 :type :arrow})                 ; an arrow about to leave the scene
(map->Obstacle {:x 342 :type :gate})                  ; a gate

(defn fn-for-obstacle [{:keys [x type] :as obstacle}]
  (... x type))

;; ===============================
;; Function Definitions:

(declare on-show! init-screen!
         on-render update-entities render-entities!
         on-timer
         on-key-down
         on-begin-contact
         ui-screen main-screen)

(defn on-resize [screen entities]
  (play-clj/height! screen 600))

;; Screen Entities -> Entities
;; initialize screen rendering and create background and player entities
;; !! add tests again

(defn on-show! [screen _]
  (init-screen! screen)
  [(merge (g2d/texture background-image)
          {:background? true
           :x 0})
   (merge (g2d/texture (first (player-animations :running)))
          {:player? true
           :y-velocity 0
           :can-jump? true
           :x player-x
           :y floor-y
           :animations (reduce-kv #(assoc %1 %2 (g2d/animation 0.1 (map g2d/texture* %3)))
                                  {} player-animations)}
          (map->Player {:health        initial-health
                        :score         0
                        :current-animation :running}))])

;; Screen !-> Screen
;; initialize the screen
;; !!! not testing this for now, as side-effects of `update!` are a PITA
(defn init-screen! [screen]
  (play-clj/graphics! :set-v-sync true)
  (play-clj/graphics! :set-title game-title)

  (-> screen
      (play-clj/update! :renderer (play-clj/stage)
                        :camera (play-clj/orthographic))
      (play-clj/add-timer! :spawn-obstacle 1 obstacle-interval)))

;; Screen Entities -> Entities
;; update world state and render entities, produce updated entities
(defn on-render [screen entities]
  (->> entities
       (update-entities screen)
       (render-entities! screen)))

;; Entities -> Entities
;; produce an updated world state
;; !!!
(defn update-entities [screen entities]
  (->> entities
       (map (fn [entity]
              (cond (:player? entity)
                    (let [new-y (+ (:y entity) (:y-velocity entity))]
                      (play-clj/screen! ui-screen :on-change-player :health (:health entity)
                                        :score (:score entity))
                      (-> entity
                          (assoc :y (max new-y floor-y))
                          (update :y-velocity #(if (<= (:y entity) floor-y)
                                                 0
                                                 (+ % gravity)))
                          (assoc :can-jump? (<= (:y entity) floor-y))
                          (update :current-animation #(if (<= new-y floor-y)
                                                        :running
                                                        %))
                          (merge (g2d/animation->texture screen (get-in entity [:animations (:current-animation entity)])))))

                    (:background? entity)
                    (if (< (:x entity) (- (/ (g2d/texture! entity :get-region-width) 1.5)))
                      (assoc entity :x 0)
                      (update entity :x - player-speed))

                    (:obstacle? entity)
                    (when-not (< (:x entity) obstacle-destroy-x)
                      (-> entity
                          (update :x - (+ player-speed obstacle-speed))
                          (merge (g2d/animation->texture screen (:animation entity)))))

                    :else entity)))
       (remove nil?)
       vec))

;; Entities !-> Entities
;; render entities, returns the original entities
(defn render-entities! [screen entities]
  (play-clj/clear!)
  (play-clj/render! screen entities))

;; Screen Entities -> Entities
;; produce updated world state on timed events
;; !!!
(defn on-timer [screen entities]
  (case (:id screen)
    :spawn-obstacle
    (conj entities (merge (g2d/texture (first barrel-animation))
                          {:x (play-clj/width screen)
                           :y floor-y
                           :animation (g2d/animation 0.1 (map g2d/texture* barrel-animation))
                           :obstacle? true
                           :angle 0}))

    entities))

;; Screen Entities -> Entities
;; produce updated world state on key inputs
;; !!!
(defn on-key-down [screen entities]
  (condp = (:key screen)
    (play-clj/key-code :dpad-up)
    (->> entities
         (map (fn [entity]
                (if (and (:player? entity) (:can-jump? entity))
                  (-> entity
                      (assoc :y-velocity player-max-y-velocity
                             :current-animation :jumping))
                  entity))))
    entities))

;; Screen Entities -> Entities
;; produce updated world state on collision of entities
;; !!!
(defn on-begin-contact [screen entities]
  entities)

;; the main game screen
(play-clj/defscreen main-screen
  :on-show on-show!                   ; Screen Entities -> Entities
  :on-render on-render                ; Screen Entities -> Entities
  :on-timer on-timer                  ; Screen Entities -> Entities
  :on-key-down on-key-down            ; Screen Entities -> Entities
  :on-begin-contact on-begin-contact  ; Screen Entities -> Entities
  :on-resize on-resize)

;; the UI overlay for the main game screen
;; !!!
(play-clj/defscreen ui-screen
  :on-show
  (fn [screen entities]
    (play-clj/update! screen :camera (play-clj/orthographic) :renderer (play-clj/stage))
    [(merge (ui/label "" (play-clj/color :red))
            {:id :health
             :x (* 0.05 (play-clj/game :width))
             :y (* 0.95 (play-clj/game :height))})
     (merge (ui/label "" (play-clj/color :orange))
            {:id :score
             :x (* 0.95 (play-clj/game :width))
             :y (* 0.95 (play-clj/game :height))})
     (merge (ui/label "" (play-clj/color :black))
            {:id :fps
             :x (* 0.05 (play-clj/game :width))
             :y (* 0.05 (play-clj/game :height))})])
  :on-resize on-resize
  :on-change-player
  (fn [{:keys [health score]} entities]
    (->> (for [entity entities]
           (case (:id entity)
             :health (doto entity
                       (ui/label! :set-text (str health)))
             :score (doto entity
                      (ui/label! :set-text (str score)))
             entity))))
  :on-render
  (fn [screen entities]
    (->> (for [entity entities]
           (case (:id entity)
             :fps (doto entity
                    (ui/label! :set-text (str (play-clj/graphics! :get-frames-per-second) "fps")))
             entity))
         (play-clj/render! screen))))

(play-clj/defgame viking-village-vandalism-game
  :on-create
  (fn [this]
    (play-clj/set-screen! this main-screen ui-screen)))

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
  (play-clj/on-gl (play-clj/set-screen! viking-village-vandalism-game main-screen ui-screen))

  (play-clj/on-gl (play-clj/set-screen! viking-village-vandalism-game blank-screen))

  )
