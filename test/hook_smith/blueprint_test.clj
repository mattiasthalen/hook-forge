(ns hook-smith.blueprint-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [hook-smith.blueprint :as blueprint]
            [hook-smith.utilities :as utilities]
            [babashka.fs :as fs]))

;; Create a dynamic var for the temp directory
(def ^:dynamic *test-temp-dir* nil)

;; Fixture for temporary directory
(defn temp-dir-fixture [f]
  (let [tmp-dir (fs/create-temp-dir {:prefix "blueprint-test"})]
    (try
      (binding [*test-temp-dir* (str tmp-dir)]
        (f))
      (finally
        (fs/delete-tree tmp-dir)))))

(use-fixtures :each temp-dir-fixture)

(deftest concepts-blueprint-test
  (testing "concepts blueprint has the expected structure"
    (let [concepts blueprint/concepts-blueprint]
      (is (vector? concepts))
      (is (every? map? concepts))
      (is (every? #(contains? % :name) concepts))
      (is (every? #(contains? % :type) concepts))
      (is (some #(= (:name %) "product") concepts))
      (is (some #(= (:name %) "customer") concepts))
      (is (some #(= (:name %) "order") concepts)))))

(deftest keysets-blueprint-test
  (testing "keysets blueprint has the expected structure"
    (let [keysets blueprint/keysets-blueprint]
      (is (vector? keysets))
      (is (every? map? keysets))
      (is (every? #(contains? % :name) keysets))
      (is (every? #(contains? % :concept) keysets))
      (is (every? #(contains? % :qualifier) keysets))
      (is (every? #(contains? % :source_system) keysets))
      (is (some #(= (:name %) "source.product.id") keysets))
      (is (some #(= (:name %) "source.customer.name") keysets))
      (is (some #(= (:name %) "source.order.order_number") keysets)))))

(deftest frames-blueprint-test
  (testing "frames blueprint has the expected structure"
    (let [frames blueprint/frames-blueprint]
      (is (vector? frames))
      (is (every? map? frames))
      (is (every? #(contains? % :name) frames))
      (is (every? #(contains? % :source_system) frames))
      (is (every? #(contains? % :source_table) frames))
      (is (every? #(contains? % :target_table) frames))
      (is (some #(= (:name %) "source__products") frames))
      (is (some #(= (:name %) "source__customers") frames))
      (is (some #(= (:name %) "source__orders") frames))
      (is (some #(= (:name %) "source__order_lines") frames))
      
      ;; Test hooks and composite hooks
      (let [order-lines (first (filter #(= (:name %) "source__order_lines") frames))]
        (is (contains? order-lines :hooks))
        (is (contains? order-lines :composite_hooks))
        (is (vector? (:hooks order-lines)))
        (is (vector? (:composite_hooks order-lines)))))))

(deftest uss-blueprint-test
  (testing "uss blueprint has the expected structure"
    (let [uss blueprint/uss-blueprint]
      (is (map? uss))
      (is (contains? uss :bridge_path))
      (is (contains? uss :peripherals))
      
      (let [peripherals (:peripherals uss)]
        (is (vector? peripherals))
        (is (every? map? peripherals))
        (is (every? #(contains? % :name) peripherals))
        (is (every? #(contains? % :source_table) peripherals))
        (is (every? #(contains? % :target_table) peripherals))
        (is (every? #(contains? % :valid_from) peripherals))
        (is (every? #(contains? % :valid_to) peripherals))
        (is (some #(= (:name %) "source__products") peripherals))
        (is (some #(= (:name %) "source__customers") peripherals))
        (is (some #(= (:name %) "source__orders") peripherals))
        (is (some #(= (:name %) "source__order_lines") peripherals))
        
        ;; Test events on orders
        (let [orders (first (filter #(= (:name %) "source__orders") peripherals))]
          (is (contains? orders :events))
          (is (vector? (:events orders)))
          (is (= 3 (count (:events orders))))
          (is (some #(= (:name %) "Order Placed On") (:events orders)))
          (is (some #(= (:name %) "Order Due On") (:events orders)))
          (is (some #(= (:name %) "Order Delivered On") (:events orders)))
          (is (= "order_delivered_on" (:field (last (:events orders))))))))))

(deftest generate-blueprint-file-test
  (testing "generates a YAML file from blueprint data"
    (with-redefs [utilities/safe-save (fn [path content] 
                                        (is (string? path))
                                        (is (string? content))
                                        path)]
      (let [test-path *test-temp-dir*
            blueprint-name "test-name"
            blueprint-data [blueprint-name {:key "value"}]
            result (blueprint/generate-blueprint-file test-path blueprint-data)]
        (is (= result (str test-path "/" blueprint-name ".yaml")))))))