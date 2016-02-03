(ns rts.entities.unity
  (:use arcadia.core
        folha.core
        rts.entities.core)
  (:import [UnityEngine Vector3]))

(defn destination [dest]
  (if (= Vector3 (type dest)) dest (position dest)))

(defn controllable?   [e] (-> e ->state :controllable))
(defn ->steering      [e] (-> e ->state :steering))
(defn ->destination   [e] (-> e ->steering :destination))
(defn ->speed         [e] (-> e ->steering :speed))
(defn ->selected      [e] (-> e ->state :selected))
(defn ->hp-bar        [e] (-> e ->state :hp-bar))
(defn ->select-circle [e] (-> e ->state :select-circle))

(defn id->state [id] (->state (->obj id)))

(defn sync-steering! [this]
  "Update Hook for an Entity"
  (let [agent (nav-mesh-agent* this)
        dest (->destination this)]
    (if dest (move! this (->destination this)))
    (set! (.speed agent) (->speed this))))

(defn align-hp-bar! [this]
  (position! (->hp-bar this)
             (v3+ (position this)
                  (v3 0 6 -3))))

(defn scale-selected! [this]
  (scale! (->select-circle this)
          (if (->selected this)
            (v3 10 10 10)
            (v3 0 0 0))))

(defn sync-position [this state]
  (assoc state :position (v3->clj (position this))))

;; Hooks
(defn update! [this state]
  "Update Hook for an Entity"
  (doto this
    (sync-steering!)
    (align-hp-bar!)
    (scale-selected!))
  (->> state
       (sync-position this)))

(defn start! [this type init]
  (let [hp-bar (prefab! "hp-bar")
        selected (prefab! "selected")
        st (-> (basis type init)
               (assoc :hp-bar hp-bar)
               (assoc :select-circle selected))]
    (rotation! hp-bar 0.3 0 0 0.95)
    (parent! hp-bar (the "HP Bars"))
    (parent! selected this)
    (local-position! selected 0 0 0)
    (state! this st)))
