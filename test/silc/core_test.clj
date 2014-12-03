(ns silc.core-test
  (:require [silc.core :refer :all]
            [midje.sweet :refer :all]
            [clojure.test :refer :all]))

(comment
  (require 'midje.repl)
  (midje.repl/autotest))

(facts
 "about `atts`"
 (background
  .m. =contains=> {:silc.core/eav {0 {:foo "bar", :qux? true}
                                   1 {:baz 3}}})
 
 (fact
  "`atts` simply returns all the same attributes as are in the eav index"
  (atts .m. 0) => {:foo "bar" :qux? true}
  (atts .m. 1) => {:baz 3})
 
 (fact
  "returns nil for a non-existent entity"
  (atts .m. 2) => nil
  (atts nil nil) => nil))

(facts
 "about `att`"
 (fact
  "will return the attribute at e and k if both exist in the eav index"
  (att .m. 0 :foo) => "bar"
  (provided
   (atts .m. 0) => {:foo "bar"}))
 
 (fact
  "will return the default value if the attribute does not exist"
  (att .m. 0 :foo :default) => :default
  (att .m. 0 :foo) => nil
  (provided
   (atts .m. 0) => {})))

(fact
 "`entities` takes every key from the eav index"
 (fact
  (entities {:silc.core/eav {0 {} 1 {}}}) => [0 1]))

(facts
 "about `with`"
 (fact
  "if the ave index is defined, it simply takes the values from the index"
  (with .m. :foo "bar") => #{0 1}
  (with .m. :foo "wut") => #{}
  (prerequisites
   .m. =contains=> {:silc.core/ave {:foo {"bar" #{0 1}}}}))
 (fact
  "else it does a linear scan by using entities and atts"
  (with .m. :foo "bar") => #{0 1}
  (with .m. :something :else) => #{2}
  (with .m. nil nil) => #{}
  (prerequisites
   (atts .m. 0) => {:foo "bar"}
   (atts .m. 1) => {:foo "bar"}
   (atts .m. 2) => {:something :else}
   (entities .m.) => #{0 1 2})))

(fact
 "`all` is exactly the same as (with m a true)"
 (all .m. :qux?) => #{0 1 2}
 (provided (with .m. :qux? true) => #{0 1 2}))

(facts
 "about `delete-att`"
 (fact
  "completely removes the attribute from the eav index"
  (let [m {:silc.core/eav {0 {:foo "bar"}
                           1 {:foo "bar"
                              :bar "fred"}}}]
    (delete-att m 0 :foo) => {:silc.core/eav {1 {:foo "bar"
                                                 :bar "fred"}}}
    (delete-att m 1 :foo) => {:silc.core/eav {0 {:foo "bar"}
                                              1 {:bar "fred"}}}))
 (fact
  "completely removes the entity from the ave index for that attribute
   if it also exists in the eav index."
  (let [m {:silc.core/ave {:qux? {true #{0 1 2}
                                  false #{3}}}
           :silc.core/eav {0 {:qux? true}}}]
    (delete-att m 0 :qux?) => {:silc.core/ave {:qux? {true #{1 2}
                                                      false #{3}}}})))

(fact
 "`delete` removes all entity properties from the map"
 (delete {:silc.core/eav {0 {:foo "bar", :qux? true}}} 0) => {}
 (delete {} 0) => {})

(facts
 "about `set-att`"
 (fact
  "sets the att"
  (set-att nil 0 :foo "bar") => #(= (att % 0 :foo) "bar"))
 (fact
  "you can set more than one att at a time"
  (set-att nil 0 :foo "bar" :qux? true)
  => #(= (atts % 0) {:foo "bar" :qux? true})))

(fact
 "`set-atts` merges a map or seq of key value pairs into the entity as attributes"
 (set-atts nil 0 {:foo "bar", :baz "qux"})
 => #(= (atts % 0) {:foo "bar" :baz "qux"})
 (fact "does not remove previous entries, just overwrites and adds")
 (set-atts
  (set-atts nil 0 {:foo "bar", :baz "qux"})
  0
  {:fred :ethel, :foo "hey"})
 => #(= (atts % 0) {:foo "hey", :fred :ethel, :baz "qux"}))

(fact
 "`db` creates an empty db with the index flags supplied"
 (db #{:foo, :bar}) => {:silc.core/ave? #{:foo, :bar}})

(facts
 "about `id`"
 (fact
  "simply returns 0 as the default"
  (id nil) => 0
  (id {}) => 0)
 (fact
  "returns value at :silc.core/id if its there"
  (id {:silc.core/id 42}) => 42))

(facts
 "about `create`"
 (fact
  "creates an entity and assigns attributes"
  (create nil {:foo 1, :bar "bar"}) => {:silc.core/eav {0 {:foo 1, :bar "bar"}}
                                        :silc.core/id 1}))

(facts
 "about `create-pair`"
 (fact
  "like create but returns the id as well (at time of creation)"
  (create-pair nil {:foo 1, :bar "bar"}) => [.id. .m.]
  (provided
   (create nil {:foo 1, :bar "bar"}) => .m.
   (id nil) => .id.)))

(facts
 "about `creates`"
 (fact
  "creates many entities by reducing via `create`"
  (creates nil [.atts. .atts2.]) => .m2.
  (provided
   (create nil .atts.) => .m.
   (create .m. .atts2.) => .m2.)))


(deftest test-composites
  (let [m (with-composite-indexes {} #{:foo :bar} #{:foo :baz})
        empty {:silc.core/composites {:foo #{#{:foo :bar}
                                             #{:foo :baz}}
                                      :bar #{#{:foo :bar}}
                                      :baz #{#{:foo :baz}}}
               :silc.core/ave?       #{#{:foo :bar} #{:foo :baz}}}]
    (testing "the initial map here should be empty"
      (is (= m empty)))
    (testing "if I add a value that is part of an index"
      (let [m (set-att m 0 :foo "foo")]
        (testing "I should have still set the value"
          (is (= (att m 0 :foo) "foo")))
        (testing "I should also have the composite value(s) in my eav index - it won't include a value for bar or baz yet"
          (is (= (att m 0 #{:foo :bar}) {:foo "foo"}))
          (is (= (att m 0 #{:foo :baz}) {:foo "foo"})))
        (testing "I should have the composite values in the ave index as well"
          (is (= (with m #{:foo :bar} {:foo "foo"}) #{0}))
          (is (= (with m #{:foo :baz} {:foo "foo"}) #{0})))
        (testing "lets add the other values"
          (let [m (set-atts m 0 {:bar "bar", :baz "baz"})]
            (is (= (att m 0 :bar) "bar"))
            (is (= (att m 0 :baz) "baz"))
            (is (= (att m 0 #{:foo :bar}) {:foo "foo" :bar "bar"}))
            (is (= (att m 0 #{:foo :baz}) {:foo "foo" :baz "baz"}))
            (is (= (with m #{:foo :bar} {:foo "foo" :bar "bar"}) #{0}))
            (is (= (with m #{:foo :baz} {:foo "foo" :baz "baz"}) #{0}))
            (testing "make sure the old values aren't still in the ave index"
              (is (= (with m #{:foo :bar} {:foo "foo"}) #{}))
              (is (= (with m #{:foo :baz} {:foo "foo"}) #{})))))
        (testing "lets delete the baz"
          (let [m (delete-att m 0 :baz)]
            (is (nil? (att m 0 :baz)))
            (is (= (att m 0 #{:foo :baz}) {:foo "foo"}))
            (testing "and finally clean up the entity, returning to my empty state"
              (is (= (delete m 0) empty)))))))))