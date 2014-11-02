(ns silc.core
  "A simple entity datastore for clojure")

;;util

(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

;;db

(defn atts
  "Return all the attributes in the eav index for an entity as a map"
  [m e]
  (-> m ::eav (get e)))

(defn att
  "Returns an attribute value for the given entity.
   an else value can be specified to be returned if the attribute or entity cannot be found"
  ([m e a]
     (att m e a nil))
  ([m e a else]
     (get (atts m e) a else)))

(defn- ave
  [m a v]
  (-> m ::ave (get a) (get v)))

(defn entities
  "Return a seq of every entity in the map"
  [m]
  (keys (::eav m)))

(defn with
  "Returns the set of all entities having attribute with the specified value"
  [m a v]
  (or (ave m a v)
      (set (for [e (entities m)
                 [a val] (atts m e)
                 :when (= v val)]
             e))))

(defn all
  "Like (with m a true)"
  [m a]
  (with m a true))

(defn- unindex-ave
  "Removes the entity from the ave index for attribute/value"
  [m e a v]
  (let [current (ave m a v)
        set (disj current e)]
    (if (empty? set)
      (dissoc-in m [::ave a v])
      (assoc-in m [::ave a v] set))))

(defn- unindex
  "Removes the given e a v triple from `m` by deleting it from any indexes"
  [m e a v]
  (cond->
   m
   (contains? (ave m a v) e) (unindex-ave e a v)
   :always (dissoc-in [::eav e a])))

(defn delete-att
  "Removes the given attribute from the entity"
  [m e a]
  (unindex m e a (att m e a)))

(defn delete
  "Removes the given entity and attributes entirely from all indexes"
  [m e]
  (let [atts (keys (atts m e))]
    (reduce #(delete-att %1 e %2) m atts)))

(defn- index
  "Adds the given e a v triple to `m` indexing if it has been configured for the attribute."
  [m e a v]
  (cond->
   m
   (contains? (::ave? m) a) (update-in [::ave a v] (fnil conj #{}) e)
   :always (assoc-in [::eav e a] v)))

(declare set-atts)

(defn set-att
  "Sets the attribute on entity `e` to a new value `v`"
  ([m e a v]
     (-> (delete-att m e a)
         (index e a v)))
  ([m e a v & {:as avs}]
     (set-atts m e (cons [a v] avs))))

(defn set-atts
  "Sets the attributes on entities to be equal"
  [m e avs]
  (reduce #(apply set-att %1 e %2) m avs))

(defn db
  "Creates a new database when provided with a set of attributes
   to index by value"
  [index-by-value]
  {::ave? index-by-value})

(defn id
  "Returns the next id that would be used to create a new entity"
  [m]
  (::id m 0M))

(defn create
  "Creates an entity, assigning the next id as the entity (as of `id`)
   takes a map of initial attributes to assign"
  ([m attmap]
     (-> (set-atts m (id m) attmap)
         (update-in [::id] (fnil inc 0M)))))

(defn create-pair
  "Like `create` but returns a tuple of the assigned id and new db"
  [m atts]
  [(id m) (create m atts)])

(defn creates
  "Takes a coll of maps and creates an entity for each one - using the map
   as an initial source of attributes"
  [m coll]
  (reduce create m coll))
