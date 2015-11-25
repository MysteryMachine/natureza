(ns natu.controller
  (:use arcadia.core
        play.core
        [natu.entities :exclude [start!]]
        natu.intities)
  (:import [UnityEngine Ray
            Physics RaycastHit]))

(defn ->selected [this] (-> this state :selected))
(defn ->target   [this] (-> this state :target))
(defn ->intities [this] (-> this state :intities))
(defn ->entities [this] (map ->obj (->intities this)))
(defn ->intity-map [intities]
  (reduce #(assoc %1 %2 (id->intity %2)) {} intities))

(defn create! [this name & {:as args}]
  (let [prefab (prefab! name)
        id (->id prefab)
        intities (->intities this)]
    (parent! prefab this)
    (swat! prefab #(merge % args))
    (swat! this #(assoc % :intities (conj intities id))))
  this)

(defn is-selected? [this id] (get (->selected this) id))

(defn handle-controls! [this]
  (if (right-click)
    (if-let [hit (mouse->hit #(parent? % this))]
      (swat! this #(assoc % :target hit))
      (swat! this #(assoc % :target nil)))
    (swat! this #(assoc % :target nil)))
  (when (left-click)
    (let [entities  (child-components this)
          owned     (filter controllable? entities)
          owned-ids (into #{} (map ->id owned))]
      (swat! this #(assoc % :selected owned-ids))))
  this)

(defn retarget [intity target selected?]
  (if (and target selected? (:controllable intity))
    (assoc-in intity [:steering :destination] target)
    intity))

(defn update-intities! [this]
  (let [intities (->intities this)]
    (doseq [[id intity] (reduce update-map
                                (->intity-map intities)
                                intities)]
      (state! (->obj id) intity))))

(defn sync-in! [this target selected]
  (doseq [entity (->entities this)]
    (let [selected? (->id entity)
          new-intity
          (-> (state entity)
              (assoc :position (position entity))
              (retarget target selected?))]
      (state! entity new-intity)))
  this)

;; Hooks
(defn update! [this]
  (-> this
      (handle-controls!)
      (sync-in! (->target this) (->selected this))
      (update-intities!)))

(defn start! [this]
  (state! this {:intities #{} :target nil :selected nil})
  (-> this
      (create! "minotaur"
               :controllable true)))
