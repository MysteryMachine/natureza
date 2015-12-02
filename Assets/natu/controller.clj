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
(defn ->selection-box [this] (-> this state :selection-box))

(defn is-selected? [this id] (get (->selected this) id))
(defn click? [this] (= :click (->lctrl this)))
(defn drag? [this] (= :drag (->lctrl this)))

(defn create! [this name & {:as args}]
  (let [prefab   (prefab! name)
        id       (->id prefab)
        intities (->intities this)]
    (parent! prefab this)
    (swat! prefab #(merge % args))
    (swat! this #(assoc % :intities (conj intities id))))
  this)

(defn start-click! [this]
  (let [mpos (v2mpos)]
    (swat! this
     #(-> %
          (assoc :lctrl  :click)
          (assoc :ctrlpt mpos)))))

(defn half-dims [] (v2* (scrn-dims) 0.5))

(defn corners [ctrlpt1 ctrlpt2]
  (let [x1 (.x ctrlpt1) x2 (.x ctrlpt2)
        y1 (.y ctrlpt1) y2 (.y ctrlpt2)]
    [(v2 (<of x2 x1) (<of y2 y1))
     (v2 (>of x2 x1) (>of y2 y1))]))

(defn get-box-vecs [ctrlpt1 ctrlpt2]
  (let [[a b] (corners ctrlpt1 ctrlpt2)]
    [(v2- b a)
     (v2* (v2+ a b (v2- (scrn-dims))) 0.5)]))

(defn try-drag! [this]
  (let [ctrlpt2 (v2mpos)
        ctrlpt1 (->ctrlpt this)]
    (when (not= ctrlpt2 ctrlpt1)
      (swat! this #(assoc % :lctrl :drag))
      (let [[sd ap] (get-box-vecs ctrlpt1 ctrlpt2)
            selection-box (->selection-box this)]
        (set! (.anchoredPosition selection-box) ap)
        (set! (.sizeDelta selection-box) sd)))))

(defn reset-selection! [this]
  (set! (.sizeDelta (->selection-box this)) (v2 0 0)))

(defn click-over! [this]
  (reset-selection! this)
  (when-let [hit (mouse->hit controllable?)]
    (swat! this #(assoc % :selected #{(->id hit)}))))

(defn selectbox-center [this]
  (scrn->worldpt
   (v2+
    (half-dims)
    (.anchoredPosition (->selection-box this)))))

(defn selectbox-dims [this]
  (let [rect (.rect (->selection-box this))
        hfdm (half-dims)
        p1 (v2+ hfdm (v2 (.x rect) (.y rect)))
        p2 (v2+ hfdm (v2 (.xMax rect) (.yMax rect)))]
     (v3- (scrn->worldpt p2)
          (scrn->worldpt p1))))

(defn drag-over! [this]
  (let [hits (boxcast (selectbox-center this)
                      (selectbox-dims this)
                      controllable?)
        entities (map #(.transform %) hits)
        owned-ids (into #{} (map ->id entities))]
    (reset-selection! this)
    (swat! this #(assoc % :selected owned-ids))))

(defn mouse-up! [this]
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
    (left-up)    (mouse-up! this))
  this)

(defn update-intities! [this]
  (let [intities     (->intities this)
        i-map        (->intity-map intities)
        new-intities (reduce update-map i-map intities)]
    (doseq [[id intity] new-intities]
      (state! (->obj id) intity))))

(defn retarget [intity target selected?]
  (if (and target selected? (:controllable intity))
    (assoc-in intity [:steering :destination] target)
    intity))

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
  "Update Hook for an RTS Controller"
  (-> this
      (handle-controls!)
      (sync-in! (->target this) (->selected this))
      (update-intities!)))

(defn start! [this]
  "Start Hook for an RTS Controller"
  (state! this {:intities #{}
                :target nil
                :selected nil
                ;; TODO: Create the selection box inside here
                :selection-box (the "Selection Box" "RectTransform")})
  (-> this
      (create! "minotaur"
               :controllable true)))
