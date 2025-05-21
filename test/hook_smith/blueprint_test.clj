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
      (is (every? #(contains? % :source_path) frames))
      (is (every? #(contains? % :target_path) frames))
      (is (some #(= (:name %) "frame__source__products") frames))
      (is (some #(= (:name %) "frame__source__customers") frames))
      (is (some #(= (:name %) "frame__source__orders") frames))
      (is (some #(= (:name %) "frame__source__order_lines") frames))
      
      ;; Test hooks and composite hooks
      (let [order-lines (first (filter #(= (:name %) "frame__source__order_lines") frames))]
        (is (contains? order-lines :hooks))
        (is (contains? order-lines :composite_hooks))
        (is (vector? (:hooks order-lines)))
        (is (vector? (:composite_hooks order-lines)))))))

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