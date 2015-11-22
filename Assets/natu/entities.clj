(ns natu.entities
  (:use arcadia.core
        play.core)
  (:import [UnityEngine Vector3]))

(def intities (atom {}))

(declare sync-steering!)
(declare new-id)

(defcomponent Entity [^Int32 id]
  (Awake  [^Entity this] (new-id this))
  (Update [^Entity this] (sync-steering! this)))

(def new-id
  (let [id-gen (atom 0)]
    (fn [^Entity this]
      (let [id (int (inc @id-gen))
            new-intities (assoc @intities id {})]
        (reset! id-gen id)
        (set! (.id this) id)
        (reset! intities new-intities)))))

(defn destination [dest]
  (if (= Vector3 (type dest)) dest (position dest)))

(defn ->id [^Entity e] (.id e))
(defn ->intity [^Entity e] (get @intities (->id e)))
(defn ->intity! [^Entity e change]
  (reset! intities (assoc @intities (->id e) change)))

(defn controllable [^Entity e] (-> e ->intity :controllable))
(defn sync-steering! [^Entity this]
  (let [agent (nav-mesh-agent* this)
        steering (-> this ->intity :steering)
        dest (:destination steering)]
    (if dest (move! this (destination dest)))
    (set! (.speed agent) (:speed steering))))

(defprotocol IntityProtocol
  (update-map [this intity-map]))

(defrecord Rat [])
(extend-type Rat
  IntityProtocol
  (update-map [this intity-map] intity-map))
(def rat-basis
  (map->Rat
   {:steering {:speed 4}
    :hp 10}))

(defrecord Minotaur [])
(extend-type Minotaur
  IntityProtocol
  (update-map [this intity-map] intity-map))
(def minotaur-basis
  (map->Minotaur
   {:steering {:speed 2}
    :hp 10}))

(def basis
  {:minotaur minotaur-basis 
   :rat      rat-basis})

(defn build! [name & {:as args}]
  (let [go     (prefab! name)
        ^Entity entity (the go Entity)]
    (->intity! entity (into (basis (keyword name)) args))))
