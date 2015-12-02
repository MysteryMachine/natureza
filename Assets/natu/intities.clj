(ns natu.intities)

;; An intity is the internal, Clojure representation of
;; an entity. The Entity is the outer Unity shell.

(defmulti basis :type)
(defn merge-basis [basis this] (merge basis (dissoc this :type)))

(defmulti update-map (fn [env id] (type (env id))))

(defrecord Rat [])
(defmethod basis :rat [init]
  (merge-basis
   (map->Rat
    {:steering {:speed 4.}
     :hp 10})
   init))
(defmethod update-map Rat [env id] env)

(defrecord Minotaur [])
(defmethod basis :minotaur [init]
  (merge-basis
   (map->Minotaur
    {:steering {:speed 2.}
     :hp 10})
   init))
(defmethod update-map Minotaur [env id] env)
