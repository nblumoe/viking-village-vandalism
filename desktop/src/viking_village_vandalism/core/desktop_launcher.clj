(ns viking-village-vandalism.core.desktop-launcher
  (:require [viking-village-vandalism.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. viking-village-vandalism-game "viking-village-vandalism" 800 600)
  (Keyboard/enableRepeatEvents true))
