# silc

A small map based datastore for clojure for managing entity identities and attributes - where sometimes those entities need to be retrieved by attribute value, or multiple attribute values quickly.

I have found this useful for the basis of a lightweight entity-component system for games.

The core is 3 indexes, `eav` and `ave` and `composite`.
- `eav` is a simple index of entity-id to map of attributes. This index is maintained for every attribute.
- `ave` is an index of attribute to value to set of entites with that attribute value pair. This index is optional.
- the `composite` index maintains dummy attributes that are sets - so if you have two attributes :foo, :bar and are interested in looking up all entities where :foo is "fred" and :bar is "ethel" for example - you can do so as O(log32 N) operation. These indexes are relatively expensive maintain and are of course optional.

## Usage

```clojure
[silc "0.1.1"]
```

```clojure
(require ['silc.core :refer :all])
```
###Create a db

Create an initial db using the `db` fn
(optional, you can use nil or an existing map if you do not wish any special indexing).

```clojure
;;pass in a set of attributes that you wish to cause indexing by value.
(db #{:foo, :qux?})
;; => {:silc.core/ave? #{:foo, :qux?}}
;; - the db is just a map, all the keys created by silc are namespaced.
```

If you wish to maintain composite indexes you have to add them when you create the db with the `with-composite-indexes` fn.

```clojure
(with-composite-indexes (db #{:foo :qux?}) #{:foo :bar})
```

*Which attributes to index **must** be decided at the time the db is empty, you change the indexing strategy once entities are created*

### Adding entities

Add an entity or entities to the db via the `create`, `creates` or `create-pair` functions:

```clojure
(def mydb (-> (db #{:foo, qux?})
              (with-composite-indexes #{:foo :bar})))
              
;;create a single entity
(create mydb {:foo "bar", :qux? true})

;;you can also create multiple entities at a time
(creates mydb [{:foo "bar"}, {:baz "dfsdf"}])

;;if you are interested in the id that was assigned 
(create-pair mydb {:baz "bar"})
;; => [0M, (the-db)]
```

###Query 

`entities` simply returns all the entity identites.
```clojure
(def mydb2 (creates mydb [{:foo "foo", :qux? true, :bar "bar0"}
                          {:foo "foo", :qux? true, :bar "bar1"}])) ;;db of 2 entities
                          
(entities mydb2) ;; => (0M, 1M) 
```
`atts` returns all the entities of a given entity as a map, including the composite entries introduced 
by the composite index. `att` simply returns the attribute value.
```clojure
(atts mydb2 0M) ;; => {:foo "foo", :qux? true, #{:foo :bar} {:foo "foo", :bar "bar0"}}

;;returns the value for the attribute
(att mydb2 1M :foo) ;; => "foo"
(att mydb2 1M :not-there :default) ;; => :default
```

The `with` fn returns all entities having an attribute with a particular value, using the ave index if possible.
```clojure
(with mydb2 :qux? true) ;; => #{0M, 1M}
(with mydb2 :foo "foo") ;; => #{0M, 1M}
(with mydb2 :wut? :could-be-anything) ;; => #{}
```
The `with` fn can be used with a composite value - provided it is indexed
```clojure
(with mydb2 #{:foo :bar} {:foo "foo", :bar "bar0"}) ;; => #{0M}
```

If you have boolean values or flags, you can use the `all` fn as a shortcut. it finds all entities where the attribute value is true. Not truthy, literally `true`.

```clojure
(all mydb2 :qux?) => ;; =>  #{0M, 1M}
```


### 'Change' entities and their attributes

```clojure
(set-att mydb2 0M :foo "wut"} ;; sets a single attribute to the value
(set-att mydb2 0M :foo "wut", :bar 42} ;; overloaded on arity

;; merge in a single map
(set-atts mydb2 0M {:bar 42, :fred :ethel})
```

###Delete entities

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
