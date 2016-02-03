(ns rts.entities.core)

(defmulti basis (fn [type _] type))
(defmulti update-map (fn [env id] (type (env id))))

(defrecord Rat [])
(defmethod basis :rat [type init] 
  (merge
   (map->Rat
    {:steering {:speed 4.}
     :hp 10
     :fov 1})
   init))
(defmethod update-map Rat [env id] env)

(defrecord Minotaur [])
(defmethod basis :minotaur [type init]
  (merge
   (map->Minotaur
    {:steering {:speed 2.}
     :hp 10
     :fov 1})
   init))
(defmethod update-map Minotaur [env id] env)
