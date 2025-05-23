(ns hook-smith.uss-test
  (:require [clojure.test :refer [deftest is testing]]
            [hook-smith.uss :as uss]))

(def test-frames
  [{:name "source__products"
    :source_table "lib://adss/das/source/raw__source__products.qvd"
    :target_table "lib://adss/dab/source/frame__source__products.qvd"
    :validity_fields {:valid_from "Record Valid From"
                      :valid_to "Record Valid To"}
    :hooks [{:name "hook__product__id"
             :primary true
             :concept "product"
             :qualifier "id"
             :keyset "source.product.id"
             :business_key_field "id"}]}
   
   {:name "source__customers"
    :source_table "lib://adss/das/source/raw__source__customers.qvd"
    :target_table "lib://adss/dab/source/frame__source__customers.qvd"
    :validity_fields {:valid_from "Record Valid From"
                      :valid_to "Record Valid To"}
    :hooks [{:name "hook__customer__id"
             :primary true
             :concept "customer"
             :qualifier "id"
             :keyset "source.customer.id"
             :business_key_field "id"}]}
   
   {:name "source__orders"
    :source_table "lib://adss/das/source/raw__source__orders.qvd"
    :target_table "lib://adss/dab/source/frame__source__orders.qvd"
    :validity_fields {:valid_from "Record Valid From"
                      :valid_to "Record Valid To"}
    :hooks [{:name "hook__order__id"
             :primary true
             :concept "order"
             :qualifier "id"
             :keyset "source.order.id"
             :business_key_field "id"}
            {:name "hook__customer__id"
             :primary false
             :concept "customer"
             :qualifier "id"
             :keyset "source.customer.id"
             :business_key_field "customer_id"}]}])

(deftest build-hook-to-frame-map-test
  (testing "should correctly map primary hooks to their frame names"
    (let [hook-map (uss/build-hook-to-frame-map test-frames)]
      (is (= "source__products" (get hook-map "hook__product__id")))
      (is (= "source__customers" (get hook-map "hook__customer__id")))
      (is (= "source__orders" (get hook-map "hook__order__id")))
      (is (nil? (get hook-map "nonexistent_hook"))) ;; Verify non-existent hooks return nil
      (is (nil? (get hook-map "hook__customer__id2"))) ;; Verify non-primary hooks aren't included
      )))

(deftest build-dependency-graph-test
  (testing "should correctly build dependency graph between frames"
    (let [hook-map (uss/build-hook-to-frame-map test-frames)
          graph (uss/build-dependency-graph test-frames hook-map)]
      (is (empty? (get graph "source__products")))
      (is (empty? (get graph "source__customers")))
      (is (= ["source__customers"] (get graph "source__orders")))
      )))

(deftest process-order-test
  (testing "should correctly determine processing order based on dependencies"
    (let [hook-map (uss/build-hook-to-frame-map test-frames)
          graph (uss/build-dependency-graph test-frames hook-map)
          order (uss/process-order graph)]
      ;; sources with no dependencies should come first
      (is (< (.indexOf order "source__products") (.indexOf order "source__orders")))
      (is (< (.indexOf order "source__customers") (.indexOf order "source__orders")))
      )))

(deftest parse-hook-name-test
  (testing "should correctly extract concept and qualifier from hook name"
    (let [result (uss/parse-hook-name "hook__product__id")]
      (is (= "product" (:concept result)))
      (is (= "id" (:qualifier result))))
    
    (testing "returns nil for invalid hook names"
      (is (nil? (uss/parse-hook-name "invalid_hook")))
      (is (nil? (uss/parse-hook-name "invalid__hook"))))))

(deftest generate-header-test
  (testing "should generate valid USS header"
    (let [header (uss/generate-uss-header)]
      (is (string? header))
      (is (.contains header "TRANSFORMING: Generating Puppini Bridge"))
      (is (.contains header "[_bridge]:"))
      (is (.contains header "NoConcatenate")))))

(deftest generate-bridge-table-load-test
  (testing "should generate valid bridge table load script"
    (let [frame (first test-frames)
          script (uss/generate-bridge-table-load frame [])]
      (is (string? script))
      (is (.contains script "[bridge__source__products]:"))
      (is (.contains script "Hash256([hook__product__id]"))
      (is (.contains script "As [pit_key__product__id]")))))

(deftest generate-puppini-bridge-test 
  (testing "should generate a complete USS script"
    (let [script (uss/generate-puppini-bridge test-frames)]
      (is (string? script))
      (is (.contains script "TRANSFORMING: Generating Puppini Bridge"))
      (is (.contains script "[bridge__source__products]:"))
      (is (.contains script "[bridge__source__customers]:"))
      (is (.contains script "[bridge__source__orders]:"))
      (is (.contains script "As [pit_key__product__id]"))
      (is (.contains script "As [pit_key__customer__id]"))
      (is (.contains script "As [pit_key__order__id]"))
      (is (.contains script "Left Join([bridge__source__orders])"))
      (is (.contains script "Finalizing bridge")))))
