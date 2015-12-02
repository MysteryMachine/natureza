(ns natu.entities
  (:use arcadia.core
        play.core
        natu.intities)
  (:import [UnityEngine Vector3])) 

(defn destination [dest]
  (if (= Vector3 (type dest)) dest (position dest)))

(defn controllable?  [e] (-> e state :controllable))
(defn ->steering     [e] (-> e state :steering))
(defn ->destination  [e] (-> e ->steering :destination))
(defn ->speed        [e] (-> e ->steering :speed))

(defn id->intity [id] (state (->obj id)))

;; Hooks
(defn sync-steering! [this]
  "Update Hook for an Entity"
  (let [agent (nav-mesh-agent* this)
        dest (->destination this)]
    (if dest (move! this (->destination this)))
    (set! (.speed agent) (->speed this))))

(defn start!
  "Start Hook for an Entity"
  [this] (swat! this basis))
