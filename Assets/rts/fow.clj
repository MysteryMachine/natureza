(ns rts.fow
  (:use folha.core
        arcadia.core
        [rts.controller :exclude [start! update!]])
  (:import [UnityEngine
            TextureWrapMode]))

;; Working off this http://www.twigwhite.com/blog/2015/6/14/fog-of-war-in-unity-3d-part-one-of-three

(def fow-color (color 0 0 0 0.85))
(def clear-color (color 0 0 0 0))
(def fow-size 64)
(def fow-center-px (v2 (/ fow-size 2) (/ fow-size 2)))

(defn clear-pixels! [tex]
  (doseq [i (range (.height tex))
          j (range (.width tex))]
    (.SetPixel tex i j fow-color)))

(defn pythag [i j] (+ (* i i) (* j j)))

;; Consider creating versions that just cache valid
;; values for certain radii
(defn create-circle! [tex ppi radius x y]
  (let [max-r (* ppi radius)
        min-r (- max-r)
        r-sq (* max-r max-r)
        circle-iter (range min-r max-r)]
    (doseq [i circle-iter
            j circle-iter]
      (if (<= (pythag i j) r-sq)
        (.SetPixel tex (+ i x) (+ j y) clear-color)))))

(defn start! [this]
  (let [state (->state this)
        renderer (the this "Renderer")
        material (.sharedMaterial renderer)
        tex (texture fow-size fow-size)]
    (set! (.name tex) "FoW Texture")
    (set! (.wrapMode tex) TextureWrapMode/Clamp)
    (clear-pixels! tex)
    (.Apply tex)
    (set! (.mainTexture material) tex)
    (state! this
     {:ppi (int (/ fow-size (.. this transform lossyScale x)))
       :center-px fow-center-px
       :layer 8})))

(defn update! [this]
  (let [{:keys [ppi center-px] :as state} (->state this)
        tex (.. (the this "Renderer") sharedMaterial mainTexture)
        es (->entities (the "Controller"))]
    (clear-pixels! tex)
    (doseq [entity (filter #(:controllable (->state %)) es)]
      (let [screen-pt (obj->scrnpt entity)
            ray (scrnpt->ray screen-pt)
            hit (first (raycast ray :layer-mask (:layer state)))]
        (when hit
          (let [translated-pos (v3- (.point hit) (position this))
                px-pos-x (int (+ (* (.x translated-pos) ppi) (.x center-px)))
                px-pos-y (int (+ (* (.z translated-pos) ppi) (.y center-px)))]
            (create-circle! tex ppi 10 px-pos-x px-pos-y)))))
    (.Apply tex)))
