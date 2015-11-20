(ns natu.entities
  (:use arcadia.core
        play.core))

(declare sync-steering!)
(declare new-id)

(defcomponent Entity [^Int32 id intity ^Boolean controllable]
  (Awake  [^Entity this] (new-id this))
  #_(Update [^Entity this] (sync-steering! this)))

(defonce new-id
  (let [id-gen (atom 0)]
    (fn [^Entity this]
      (set! (.id this) (int (swap! id-gen inc))))))

(defn ->id     [^Entity e] (.id e))
(defn ->intity [^Entity e] (.intity e))
(defn sync-steering! [^Entity this]
  (let [agent (nav-mesh-agent* this)
        steering (-> ~this ->intity :steering)
        dest (:destination steering)]
    (if dest (move! this dest))
    (set! (.speed agent) (:speed steering))))

(defprotocol IntityProtocol
  (update-map [this intity-map]))

(defrecord Rat [])
(extend-type Rat
  IntityProtocol
  (update-map [this intity-map] intity-map))
(def rat-basis
  {:steering {:speed 4}
   :hp 10})

(defrecord Minotaur []) 
(extend-type Minotaur
  IntityProtocol
  (update-map [this intity-map] intity-map))
(def minotaur-basis
  {:steering {:speed 2}
   :hp 10})
