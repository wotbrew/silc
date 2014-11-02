# silc

A small map based datastore for clojure for managing entity identities and attributes - where sometimes those entities need to be retrieved by attribute value.

I have found this useful for the basis of a lightweight entity-component system for games.

The core is 2 indexes, eav and ave.
- eav is a simple index of entity-id to map of attributes. This index is maintained for every attribute.
- ave is an index of attribute to value to set of entites with that attribute value pair. This index is optional.

## Usage

```clojure
[silc "0.1.0-SNAPSHOT"]
```

```clojure
(require ['silc.core :refer :all])
```

Create an initial db (optional, you can use nil or an existing map if you do not wish any special indexing)

```clojure
;;pass in a set of attributes that you wish to cause in the indexing by value.
(db #{:foo, :qux?})
;; => {:silc.core/ave? #{:foo, :qux?}}
;; - the db is just a map, all the keys created by silc are namespaced.
```

Add an entity or entities to the db:

```clojure
(def mydb (db #{:foo, qux?}))

(create mydb {:foo "bar", :qux? true})
;; => {:silc.core/ave? #{:foo, qux?},
;;     :silc.core/ave {:foo  {"bar" #{0}}
;;                     :qux? {true #{0}}}
;;     :silc.core/eav {0M {:foo "bar",
;;                         :qux? true}}}

;; see how the attributes are accessible by value or by entity (as both attributes specified are indexed by value).

;;you can also create multiple entities at a time
(creates mydb [{:foo "bar}, {:baz "dfsdf"}])

;;if you are interested in the id that was assigned 
(create-pair mydb {:baz "bar"})
;; => [0M, {:silc.core/ave? {:foo, qux?} 
;;          :silc.core/eav {0M {:baz "bar"}}}]

```
Query entities and their attributes

```clojure
(def mydb2 (creates mydb [{:foo "bar", :qux? true}
                          {:foo "baz", :qux? true}])) ;;db of 2 entities

;;returns all entities
(entities mydb2) ;; => (0M, 1M) 

;;returns attributes for an entity as a map
(atts mydb2 0M) ;; => {:foo "bar", :qux? true}

;;returns the value for the attribute
(att mydb2 1M :foo) ;; => "baz"
(att mydb2 1M :not-there :default) ;; => :default

;;returns all entities having the attribute and value specified. (using the ave index if available)
(with mydb2 :qux? true) ;; => #{0M, 1M}
(with mydb2 :foo? "bar") ;; => #{0M}
(with mydb2 :wut? :could-be-anything) ;; => #{}

;;if you have boolean values or flags, you can use the all fn as a shortcut.
;;it finds all entities where the attribute value is true. Not truthy, literally `true`.
(all mydb2 :qux?) => ;; =>  #{0M, 1M}

```
Change entities and their attributes

```clojure
(set-att mydb2 0M :foo "wut"} ;; sets a single attribute to the value
(set-att mydb2 0M :foo "wut", :bar 42} ;; overloaded on arity

;; merge in a single map
(set-atts mydb2 0M {:bar 42, :fred :ethel})
```

Delete entities

```clojure
(delete mydb2 0M) 
;; deletes a single entity, removing all attributes from the eav index. 
;; also removes the entity from any ave indexed attribute value pairs.

```

Have fun!

## License

Copyright © 2014 Dan Stone

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
