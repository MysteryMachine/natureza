(ns natu.controller
  (:use arcadia.core
        play.core
        natu.entities)
  (:import [natu.entities Entity]
           [UnityEngine Ray
            Physics RaycastHit]))

(def target   (atom nil))
(def selected (atom #{}))

(declare start! update!)
(defcomponent EntityController []
  (Start  [^EntityController this] (start! this))
  (Update [^EntityController this] (update! this)))

(defn start! [^EntityController this]
  (prefab! "minotaur"))

(defn handle-controls! [^EntityController this]
  (cond
    (right-click)
    (if-let [hit (mouse->hit #(not (nil? (the* % Entity))))]
      (reset! target hit))
    (left-click)
    (let [entities (child-components this Entity)
          owned    (filter controllable entities)]
      (reset!
       selected
       (reduce conj (map ->id owned))))))

(defn is-selected [id] (get @selected id))

(defn retarget [intity id]
  (if (and (controllable intity) (is-selected intity))
      (assoc-in intity [:steering :destination] @target)))

(defn sync! [^EntityController this]
  (doseq [^Entity entity (child-components this Entity)]
    (let [intity (->intity entity)
          id     (->id entity)
          pos    (position entity)
          new-intity (-> intity
                         (assoc :position pos)
                         (retarget id))]
      (reset! intities (assoc @intities id new-intity)))))

(defn update-intities! []
  (let [is @intities]
    (reset! intities (reduce #(update-map %2 %1) is is))))

(defn reset-target! [] (reset! target nil))

(defn update! [^EntityController this]
  (handle-controls! this)
  (sync! this)
  (update-intities!)
  (reset-target!))
