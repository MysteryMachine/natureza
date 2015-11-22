(ns play.core
  (:use arcadia.core)
  (:import
   [UnityEngine  Vector3
    NavMeshAgent Animator
    Debug        Physics
    Transform    RaycastHit
    Input        Ray
    Vector3      Camera
    Resources    Quaternion]
   Caster)) 

;; Logging
(defn log [msg] (Debug/Log (str msg)))

;; Accessing Object
(defn the
  "For accessing an object based on a name and component"
  ([arg] (if (string? arg) (object-named arg) arg))
  ([obj component] (get-component (the obj) component)))

(defn the*
  [obj component]
  (.GetComponentInChildren (the obj) component))

(defn ->go [obj] (.gameObject obj))

(defn child-components [name component]
  (let [obj (the name)
        prelim (.GetComponentsInChildren obj component)]
    (if (empty? prelim)
      []
      (let [fprelim (->go (first prelim))
            ;; When I ask for a component in CHILDREN, it should
            ;; just be in the children, Unity. :U
            comps (if (= fprelim obj)
                    (rest prelim)
                    prelim)]
        comps))))

(defn children
  ([top-obj] (children top-obj identity))
  ([top-obj filter-fn]
   (let [kids (map ->go (child-components top-obj Transform))]
     (filter filter-fn kids))))

(defn ->name ^String [obj] (.name obj))

;; Vector
(defn v3
  (^Vector3 [[x y z]] (Vector3. x y z))
  (^Vector3 [x y z]   (Vector3. x y z)))

(defn q4
  (^Quaternion [[x y z a]] (Quaternion. x y z a))
  (^Quaternion [x y z a] (Quaternion. x y z a)))

(defn mag    ^Double [obj] (.magnitude obj))
(defn sqmag  ^Double [obj] (.sqrMagnitude obj))
(defn normal ^Double [obj] (.normalized obj))

(defn position ^Vector3 [obj] (.position (the obj Transform)))
(defn dist [a b] (Vector3/Distance (position a) (position b)))

;; Nav Mesh Agent
(defn nav-mesh-agent ^NavMeshAgent [obj] (the obj NavMeshAgent))
(defn nav-mesh-agent* ^NavMeshAgent [obj] (the* obj NavMeshAgent))
(defn move!
  ([obj target]
   (let [coords (if (= Vector3 (type target))
                  target
                  (position target))]
     (set! (.destination (the obj NavMeshAgent)) coords)))
  ([obj x y z] (move! obj (v3 x y z))))

;; Animator
(defn animator  ^Animator [obj] (the obj Animator))
(defn animator* ^Animator [obj] (the* obj Animator))

;; Look into maybe using a macro to define all these
;; in the future? 
(defmulti  anim-set*! #(type %3))
(defmethod anim-set*! Boolean [this ^String name arg]
  (.SetBool (animator* this) name arg))
(defmethod anim-set*! nil [this ^String name _]
  (.SetTrigger (animator* this) name))
(defmethod anim-set*! Double [this ^String name ^Double arg]
  (.SetFloat (animator* this) name (float arg)))
(defmethod anim-set*! Single [this ^String name arg]
  (.SetFloat (animator* this) name arg))
(defmethod anim-set*! Int64 [this ^String name ^Int64 arg]
  (.SetInteger (animator* this) name (int arg)))
(defmethod anim-set*! Int32 [this ^String name arg]
  (.SetInteger (animator* this) name arg))
(defmethod anim-set*! :default [this name arg]
  (throw (str "Unsure how to set animation " arg " for property " name)))

(defcomponent SpeedAnimSync [^String argName]
  (Awake [this] (set! argName "Speed"))
  (Update [this]
    (anim-set*! this argName (mag (.velocity (nav-mesh-agent* this))))))

;; Raycasting

(defn main-camera ^Camera [] (Camera/main))

(defn mouse-pos ^Vector3 [] (Input/mousePosition))

(defn right-click [] (Input/GetMouseButtonDown 1))

(defn left-click  [] (Input/GetMouseButtonDown 0))

(defn mouse->hit
  ([] (mouse->hit (fn [_] false)))
  ([filter-fn]
   (let [ray (.ScreenPointToRay (main-camera) (mouse-pos))
         caster (the "Caster" Caster)]
     (if (.Cast caster ^Ray ray)
       (let [info (.hit caster)
             go   (->go (.transform info))]
         (if (filter-fn go) go (.point info)))))))

;; Prefab
(defn clone!
  ([^GameObject obj] (GameObject/Instantiate obj))
  ([^GameObject obj ^Vector3 pos ^Quaternion rot] (GameObject/Instantiate obj pos rot)))

(defn prefab!
  ([^String name] (clone! (Resources/Load name)))
  ([^String name  ^Vector3 pos ^Quaternion rot]
   (clone! (Resources/Load name) pos rot)))

(defn parent [obj] (.parent (the obj Transform)))

(defn parent! [obj v3]
  (set! (.parent (the obj Transform)) v3))
