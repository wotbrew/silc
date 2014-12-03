# silc 

A small map based datastore for clojure for managing entity identities and their attributes. Useful when you need to manage many identities (simulations, games) but want to keep your code pure.

## Goals
- Simple interface with predictable performance
- Has to be fast enough for usage in at least simple games, so very fast, at least on reads.
- The ability to enable automatic hash based indexing for effectively constant lookup of:
 - _entity attributes._
 - _entities by attribute value or composite thereof._
 - _entities by attribute._
- The ability to trade-off memory and disable indexes if appropriate.

## Usage

Include in your `project.clj`

```clojure
[silc "0.1.2"]
```


All the functions are in the `silc.core` namespace

```clojure
(require ['silc.core :refer :all])
```

###Getting started

The database used by silc is *just a map*. You can use an existing map if you want (silc uses namespaced keys).
If you do not care about indexing - then that is all you need to do!

**NB** _ All the change, addition and deletion functions simply return a new database value. silc is completely pure..._

###Indexing

The core of silc are the 4 indexes, `eav`, `ave`, `ae` and `composite`.
- `eav` is a simple index of entity-id to map of attributes. This index is always maintained for every attribute, it is the primary index.
- **optional** `ave`is an index of attribute to value to set of entites with that attribute value pair. This index can be enabled for particular attributes.
- **optional** `ae` is an index of attributes to sets of entities having that attribute. This index is optional and is enabled for a given database as a whole.
- **optional** `composite` maintains dummy attributes that are sets - so if you have two attributes :foo, :bar and are interested in looking up all entities where :foo is "fred" and :bar is "ethel" for example - you can do so as an O(log32 N) operation. This index is enabled for particular sets of attributes.

The `db` fn is the easiest way to get a silc db with some indexing by value:
```clojure
(db #{:position, :map, :creature?}) 
;; => will create a new map with the ave index enabled for :position, :map and :creature
```

- You can enable `ave` indexing with the `with-indexes` fn
- If you wish to maintain `composite` indexing use the `with-composite-indexes` fn.
- You can enable the `ae` index with the `enable-ae-indexing` fn

Example:
```clojure
(-> {} 
    (with-indexes :position :map :creature?) ;;by value indexes will be maintained for these attributes
    (with-composite-indexes #{:position :map}) ;;composite indexes will be maintained for each set I pass
    enable-ae-indexing)) ;;ae indexing is enabled either all together, or not at all.
    ;; => returns a new silc db, ready to rock!
```


**NB** _Indexing **must** be decided at the time the db is empty, you cannot change the indexing strategy once entities are created_

### Adding entities

Add an entity or entities to the db via the `create`, `creates` or `create-pair` functions:

```clojure
(def mydb 
  (-> {} 
    (with-indexes :position :map :creature?)
    (with-composite-indexes #{:position :map})
    enable-ae-indexing)))
```
```clojure
;;create a single entity with some initial attributes via 'create'
(create mydb {:name "Fred", :position [0 0], :map :level1, :creature? true})
;; => returns a new db with the entity added - a numeric id will be assigned

;;you can also create multiple entities at a time via 'creates'
(creates mydb [{:name "Fred", :creature? true, :position [0 0], :map :level1}
               {:transition :level2, :stair? true, :position [1 0], :map :level1}])
```               

If you are interested in the id that was assigned you can find out using the `create-pair` fn.
As the db is pure you can also look at the last id (`last-id`), or next id (`id`).

```clojure
(create-pair mydb {:name "Fred", :creature? true, :position [0 0], :map :level1})
;; => [0, (the-db)]

```

###Query 

`entities` simply returns all the entity identites.
```clojure
(def mydb2 
 (creates mydb ;;using the ave, composite and ae indexes
  [{:name "Fred", :creature? true, :position [0 0], :map :level1} 
   {:transition :level2, :stair? true, :position [1 0], :map :level1}])) ;;db of 2 entities
                          
(entities mydb2) ;; => (0, 1) 
```

`att` simply returns the attribute value or the default value supplied.

```clojure
(att mydb2 0 :name) ;; => "Fred"
(att mydb2 1 :not-there) ;; => nil
(att mydb2 1 :not-there :default) ;; => :default
```

`atts` returns all the attributes of a given entity as a map, including the composite entries introduced 
by the `composite` index.
```clojure
(atts mydb2 0) 
;; => {:name "Fred", :creature? true, :position [0 0],
;;     :map :level1,
;;     #{:map, :position} {:map :level1, :position [0 0]}}}
```

The `with` fn returns all entities having an attribute with a particular value, using the `ave` index if possible.
If the `ave` index hasn't been enabled for the given attribute - a linear scan is performed.
```clojure
(with mydb2 :map? :level1) ;; => #{0, 1}
(with mydb2 :name "Fred") ;; => #{0}
(with mydb2 :wut? :could-be-anything) ;; => #{}
```
The `with` fn can be used with a composite value - provided it is indexed. This fn doesn't attempt to fall back on a linear scan like the others, so beware.
```clojure
(with mydb2 #{:map :position} {:map :level1, :position [0 0]}) ;; => #{0}
```

If you have boolean values or flags, you can use the `all` fn as a shortcut. it finds all entities where the attribute value is true. Not truthy, literally `true`.

```clojure
(all mydb2 :creature?) => ;; =>  #{0}
```

For checking which entities have an attribute you can use the `having` fn - this utilizes the `ae` index if enabled.
```clojure
(having mydb2 :position) ;; => #{0, 1}
```

### 'Change' entities and their attributes

`set-att` is the `assoc` of silc. It sets an attribute value on an entity, or introduces a new attribute. It is overloaded to take many attribute value pairs.

```clojure
(set-att mydb2 0 :poisoned? true}
(set-att mydb2 0 :foo "wut", :bar 42} ;; overloaded on arity
```

Merge in a map of attributes and values using the `set-atts` fn
```clojure
(set-atts mydb2 0 {:bar 42, :fred :ethel})
```
`delete-att` is the `dissoc` of silc. It removes an attribute from an entities.

```clojure
(delete-att mydb2 0 :name) 
```

### Delete entities

`delete` deletes a single entity and removes any trace of it and its attributes from all indexes.

```clojure
(delete mydb2 0) 
```

### TODO

- optional core.logic query support?

## Contributions welcome!

## License

Copyright © 2014 Dan Stone

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
