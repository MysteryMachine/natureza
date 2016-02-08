(ns rts.fow
  (:use folha.core
        arcadia.core
        [rts.controller :exclude [start! update!]])
  (:require [clojure.data :refer [diff]])
  (:import [UnityEngine
            TextureWrapMode
            Texture]))

;; Working off this http://www.twigwhite.com/blog/2015/6/14/fog-of-war-in-unity-3d-part-one-of-three

(set! *unchecked-math* true)

(defn clear-pixels! [^Texture tex fow-color fow-size]
  (loop [i 0 j 0]
    (cond
      (= i fow-size) :ok
      (= j fow-size) (recur (inc i) 0)
      :else (do
              (.SetPixel tex i j fow-color)
              (recur i (inc j))))))

(definline pythag [i j] `(+ (* ~i ~i) (* ~j ~j)))

(defn create-circle [radius r-sq]
  (loop [circle [], i (- radius), j (- radius)]
    (cond
      (= i radius) circle
      (= j radius) (recur circle (inc i) (- radius))
      :else (recur
             (if (< (pythag i j) r-sq)
               (conj circle [i j])
               circle)
             i
             (inc j)))))

(defn offset-circle [circle hit-pt]
  (map (fn [pt]
         [(int (+ (first pt)  (.x hit-pt)))
          (int (+ (second pt) (.z hit-pt)))])
       circle))

(defn into! [trans-map elems]
  (loop [tmap trans-map
         es elems]
    (if (seq es)
      (recur (conj! tmap (first es)) (rest es))
      tmap)))

(defn visible-pxs
  [this
   {:keys [ppi translated-pos center-px tex circle]
    :as state}
   init-hits]
  (loop [pxs (transient #{})
         hits init-hits]
    (if (seq hits)
      (recur (into! pxs (offset-circle circle (first hits)))
             (rest hits))
      (persistent! pxs))))

(defn visible-entities
  [{:keys [screen-pt layer]
    :as state}]
  (filter #(not (nil? %))
   (map
     (fn [entity]
       (let [state (->state entity)]
         (and
          (:controllable state)
          (let [screen-pt (obj->scrnpt entity)
                ray (scrnpt->ray screen-pt)
                hit (first (raycast ray :layer-mask layer))]
            (when hit (.point hit))))))
     (->entities (the "Controller")))))

;; A good use of awake is actually to provide initial values for things that
;; would otherwise require a constructor, get on that
(defn start! [this]
  (let [state (->state this)
        fow-color (color 0 0 0 0.85)
        clear-color (color 0 0 0 0)
        fow-size 64
        fow-center-px (v2 (/ fow-size 2) (/ fow-size 2))
        radius 6
        rsq (* radius radius)
        renderer (the this "Renderer")
        material (.sharedMaterial renderer)
        tex (texture fow-size fow-size)
        ppi (int (/ fow-size (.. this transform lossyScale x)))]
    (set! (.name tex) "FoW Texture")
    (set! (.wrapMode tex) TextureWrapMode/Clamp)
    (clear-pixels! tex fow-color fow-size)
    (.Apply tex)
    (set! (.mainTexture material) tex)
    (state! this
            {:ppi ppi
             :fow-color fow-color
             :clear-color clear-color
             :fow-size fow-size
             :radius radius
             :r-sq rsq
             :circle (create-circle radius rsq)
             :tex tex
             :center-px fow-center-px
             :layer (bit-shift-left 1 8)
             :px-map #{}})))

(defn update! [this]
  (let [{:keys [tex px-map clear-color fow-color] :as state} (->state this)
        hits (visible-entities state)
        nupxmap (visible-pxs this state hits)
        [dark-now light-now same] (diff px-map nupxmap)]
    (doseq [[i j] dark-now]
      (.SetPixel tex i j fow-color))
    (doseq [[i j] light-now]
      (.SetPixel tex i j clear-color))
    (state! this (assoc state :px-map nupxmap))
    (.Apply tex)))
