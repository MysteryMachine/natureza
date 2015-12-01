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
(defn ->lctrl [this] (-> this state :lctrl))
(defn ->ctrlpt [this] (-> this state :ctrlpt))
(defn is-selected? [this id] (get (->selected this) id))
(defn click? [this] (= :click (->lctrl this)))
(defn drag? [this] (= :drag (->lctrl this)))

(defn create! [this name & {:as args}]
  (let [prefab (prefab! name)
        id (->id prefab)
        intities (->intities this)]
    (parent! prefab this)
    (swat! prefab #(merge % args))
    (swat! this #(assoc % :intities (conj intities id))))
  this)

(defn try-drag! [this]
  (let [ctrlpt (mouse-pos)]
    (when (not= ctrlpt (->ctrlpt this))
      (swat! this #(assoc % :lctrl :drag)))))

(defn start-click! [this]
  (swat! this #(-> %
                  (assoc :lctrl  :click)
                  (assoc :ctrlpt (mouse-pos)))))

(defn click-over! [this]
  (when-let [hit (mouse->hit controllable?)]
    (swat! this #(assoc % :selected #{(->id hit)}))))

(defn drag-over! [this]
  (let [entities  (child-components this)
        owned     (filter controllable? entities)
        owned-ids (into #{} (map ->id owned))]
    (swat! this #(assoc % :selected owned-ids))))

(defn done-click! [this]
  (case (->lctrl this)
    :click (click-over! this)
    :drag  (drag-over! this))
  (swat! this #(assoc % :lctrl nil)))

(defn handle-controls! [this]
  (cond
    (right-click)
    (when-let [hit (mouse->hit #(parent? % this) (fn [_] true))]
      (swat! this #(assoc % :target hit)))
    (right-up) (swat! this #(assoc % :target nil))
    :else nil)
  (cond
    (left-click) (start-click! this)
    (left-held)  (try-drag! this)
    (left-up)    (done-click! this))
  this)

(defn retarget [intity target selected?]
  (if (and target selected? (:controllable intity))
    (assoc-in intity [:steering :destination] target)
    intity))

(defn update-intities! [this]
  (let [intities     (->intities this)
        i-map        (->intity-map intities)
        new-intities (reduce update-map i-map intities)]
    (doseq [[id intity] new-intities]
      (state! (->obj id) intity))))

(defn sync-in! [this target selected]
  (doseq [entity (->entities this)]
    (let [selected? (get selected (->id entity))
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
