(ns rts.squad
  (:use folha.core))

(def SPACE 4.5)

;; TODO: Not fully working, should do special stuff for last
;; h in the case where (count entities) != w*w
(defn box-squad [entities centroid-vector]
  (if (nil? centroid-vector)
    nil
    (let [w (ceil (sqrt (count entities)))
          ;; Gonna need random access...maybe a cleaner way exists?
          grid (vec (map vec (partition-all w entities)))
          h (count grid)
          {:keys [x y z]} (v3->clj centroid-vector)
          half-w (/ w -2)
          half-h (/ h -2)]
      (into {}
            (for [i (range w), j (range h)]
              [(nth (nth grid i) j)
               (v3 (+ x (* SPACE (+ half-w 0.5 i)))
                   y
                   (+ z (* SPACE (+ half-h 0.5 j))))])))))

