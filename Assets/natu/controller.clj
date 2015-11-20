(ns natu.controller
  (:use arcadia.core
        play.core
        natu.entities)
  (:import [natu.entities Entity]
           [UnityEngine Ray
            Physics RaycastHit]))

(declare update!)
(defcomponent EntityController [selected target]
  (Update [^EntityController this] (update! this)))

;; TODO: Transients
(defn- add-entity-to-map [entities ^Entity entity]
  (assoc entities (.id entity) entity))
(defn- ->entity-map [entities]
  (reduce add-entity-to-map {} entities))

(defn- add-intity-to-map [intities ^Entity entity]
  (assoc intities (.id entity) (.intity entity)))
(defn- ->intity-map [entities]
  (reduce add-intity-to-map {} entities))

(defn- update-hm [intity-map]
  (reduce #(update-map %2 %1) {} intity-map))

(defn sync-intity! [updated-map entity-map]
  (doseq [[id intity] updated-map]
    (set! (.intity ^Entity (entity-map id)) intity)))

(defn sync-position! [^Entity entity]
  (let [intity (->intity entity)
        pos (position entity)]
    (set! (.intity entity) (assoc intity :position pos))))

(defn update! [^EntityController this]
  (let [entities (child-components this Entity)]
    (doseq [entity entities] (sync-position! entity))
    #_(let [entity-map (->entity-map entities) 
          intity-map  (->intity-map entities)
          updated-map (update-hm intity-map)]
        (sync-intity! updated-map entity-map))
    (if (right-click)
      (if-let [hit (mouse->hit #(not (nil? (the* % Entity))))]
         (set! (.target this) hit)))
    (let [selected (filter #(.controllable %) entities)]
      (doseq [entity selected]
        (move! (->go entity) (.target this))))))

