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

(defn circle-pts [r [center-x center-y]]
  (let [r-range (range (- r) (inc r))
        rsq (* r r)]
    (into #{}
          (for [i r-range
                j r-range
                :when (< (+ (* i i) (* j j)) rsq)]
            [(+ center-x i) (+ center-y j)]))))

(defn into! [trans-map elems]
  (loop [tmap trans-map
         es elems]
    (if (seq es)
      (recur (conj! tmap (first es)) (rest es))
      tmap)))

(defn offset-pxs [hit circle pos]
  (let [x (.x hit)
        y (.z hit)
        pos-x (.x pos)
        pos-y (.z pos)
        x-center (int (- x pos-x))
        y-center (int (- y pos-y))]
    (map
     (fn [[i j]]
       [(+ i x-center)
        (+ j y-center)])
     circle)))

(defn visible-pxs
  [this {:keys [tex circle] :as state} init-hits]
  (let [pos (position this)]
   (loop [pxs (transient #{})
          hits init-hits]
      (if (seq hits)
        (let [px-seq (offset-pxs (first hits) circle pos)
              updated-hits (into! pxs px-seq)]
          (recur updated-hits (rest hits)))
        (persistent! pxs)))))

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
        fow-color (color 0 0 0 1)
        clear-color (color 0 0 0 0)
        mid-color (color 0 0 0 0.5)
        fow-size 1024
        center-px [(/ fow-size 2) (/ fow-size 2)]
        radius 12
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
            {:fow-color fow-color
             :clear-color clear-color
             :mid-color mid-color
             :tex tex
             :layer (bit-shift-left 1 8)
             :px-map #{}
             :circle (circle-pts radius center-px)})))

(defn update! [this]
  (let [{:keys [tex px-map clear-color fow-color mid-color]
         :as state} (->state this)
        hits (visible-entities state)
        nupxmap (visible-pxs this state hits)
        [dark-now light-now same] (diff px-map nupxmap)]
    (doseq [[i j] dark-now]
      (.SetPixel tex i j mid-color))
    (doseq [[i j] light-now]
      (.SetPixel tex i j clear-color))
    (state! this (assoc state :px-map nupxmap))
    (.Apply tex)))
