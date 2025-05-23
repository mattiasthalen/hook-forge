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

(deftest find-frame-by-name-test
  (testing "should correctly find a frame by its name"
    (let [frame (uss/find-frame-by-name test-frames "source__products")]
      (is (= "source__products" (:name frame)))
      (is (= "hook__product__id" (-> frame :hooks first :name))))
    
    (testing "should return nil for non-existent frames"
      (is (nil? (uss/find-frame-by-name test-frames "non-existent-frame"))))))

(deftest find-referenced-hook-test
  (testing "should correctly find a referenced hook in a frame"
    (let [hook-map (uss/build-hook-to-frame-map test-frames)
          orders-frame (uss/find-frame-by-name test-frames "source__orders")
          referenced-hook (uss/find-referenced-hook orders-frame "source__customers" hook-map)]
      (is (= "hook__customer__id" (:name referenced-hook)))
      (is (false? (:primary referenced-hook))))
    
    (testing "should return nil for non-existent reference"
      (let [hook-map (uss/build-hook-to-frame-map test-frames)
            products-frame (uss/find-frame-by-name test-frames "source__products")]
        (is (nil? (uss/find-referenced-hook products-frame "non-existent-frame" hook-map)))))))

(deftest get-primary-hook-name-test
  (testing "should correctly get the primary hook name from a frame"
    (let [products-frame (uss/find-frame-by-name test-frames "source__products")
          customers-frame (uss/find-frame-by-name test-frames "source__customers")
          orders-frame (uss/find-frame-by-name test-frames "source__orders")]
      (is (= "hook__product__id" (uss/get-primary-hook-name products-frame)))
      (is (= "hook__customer__id" (uss/get-primary-hook-name customers-frame)))
      (is (= "hook__order__id" (uss/get-primary-hook-name orders-frame))))))

(deftest determine-pit-key-name-test
  (testing "should correctly determine the pit key name"
    (is (= "pit_key__product__id" (uss/determine-pit-key-name "hook__product__id")))
    (is (= "pit_key__customer__id" (uss/determine-pit-key-name "hook__customer__id")))
    (is (= "pit_key__order__id" (uss/determine-pit-key-name "hook__order__id")))
    
    (testing "special case for order product id"
      (is (= "pit_key__order__product__id" (uss/determine-pit-key-name "hook__order__product__id"))))))

(deftest generate-hash-key-expression-test
  (testing "should correctly generate hash key expression"
    (let [frame-validity-fields {:valid_from "Record Valid From" :valid_to "Record Valid To"}]
      (is (= "Hash256([hook__test__id], [Record Valid From])" 
             (uss/generate-hash-key-expression nil "hook__test__id" frame-validity-fields)))
      
      (testing "with composite hook"
        (let [composite-hook {:primary true :hooks ["hook1" "hook2"]}]
          (is (= "Hash256([hook1] & '~' & [hook2], [Record Valid From])"
                 (uss/generate-hash-key-expression composite-hook "hook__test__id" frame-validity-fields))))))))

(deftest generate-hook-references-test
  (testing "should correctly generate hook references"
    (let [hook-map {"hook__customer__id" "source__customers"}
          orders-frame (uss/find-frame-by-name test-frames "source__orders")
          dependencies ["source__customers"]
          result (uss/generate-hook-references dependencies orders-frame hook-map)]
      (is (string? result))
      (is (.contains result ",\t[hook__customer__id]\tAs [hook__customer__id]"))
      
      (testing "with empty dependencies"
        (is (nil? (uss/generate-hook-references [] orders-frame hook-map)))))))

(deftest generate-left-join-statement-test
  (testing "should correctly generate left join statement"
    (let [hook-map (uss/build-hook-to-frame-map test-frames)
          result (uss/generate-left-join-statement "source__orders" "source__customers" test-frames hook-map)]
      (is (string? result))
      (is (.contains result "Left Join([bridge__source__orders])"))
      (is (.contains result "Hash256([hook__customer__id], [Record Valid From])"))
      (is (.contains result "As [pit_key__customer__id]")))))

(deftest generate-foreign-key-references-test
  (testing "should correctly generate foreign key references"
    (let [dependencies ["source__customers" "source__products"]
          result (uss/generate-foreign-key-references dependencies test-frames)]
      (is (string? result))
      (is (.contains result ",\t[pit_key__customer__id]"))
      (is (.contains result ",\t[pit_key__product__id]"))
      
      (testing "with empty dependencies"
        (is (nil? (uss/generate-foreign-key-references [] test-frames)))))))

(deftest generate-range-expression-test
  (testing "should correctly generate range expression"
    (let [frame-name "source__orders"
          dependencies ["source__customers" "source__products"]
          result (uss/generate-range-expression frame-name dependencies "RangeMax" "Record Valid From")]
      (is (string? result))
      (is (.contains result ",\tRangeMax("))
      (is (.contains result "\t\t[Record Valid From (source__orders)]"))
      (is (.contains result "\t,\t[Record Valid From (source__customers)]"))
      (is (.contains result "\t,\t[Record Valid From (source__products)]"))
      (is (.contains result "\t)\tAs [Record Valid From]")))))

(deftest generate-where-clause-test
  (testing "should correctly generate where clause"
    (let [frame-name "source__orders"
          dependencies ["source__customers" "source__products"]
          result (uss/generate-where-clause frame-name dependencies)]
      (is (string? result))
      (is (.contains result "\nWhere\n\t1 = 1"))
      (is (.contains result "[Record Valid From (source__orders)] <= [Record Valid To (source__customers)]"))
      (is (.contains result "[Record Valid To (source__orders)] >= [Record Valid From (source__customers)]"))
      (is (.contains result "[Record Valid From (source__orders)] <= [Record Valid To (source__products)]"))
      (is (.contains result "[Record Valid To (source__orders)] >= [Record Valid From (source__products)]"))
      
      (testing "with empty dependencies"
        (is (nil? (uss/generate-where-clause frame-name [])))))))

(deftest generate-frame-script-test
  (testing "should correctly generate script for a frame without dependencies"
    (let [hook-map (uss/build-hook-to-frame-map test-frames)
          dependency-graph (uss/build-dependency-graph test-frames hook-map)
          products-script (uss/generate-frame-script "source__products" test-frames dependency-graph hook-map)]
      (is (string? products-script))
      (is (.contains products-script "Adding frame__source__products"))
      (is (.contains products-script "[bridge__source__products]:"))
      (is (.contains products-script "Load * Resident [bridge__source__products]"))))
    
  (testing "should correctly generate script for a frame with dependencies"
    (let [hook-map (uss/build-hook-to-frame-map test-frames)
          dependency-graph (uss/build-dependency-graph test-frames hook-map)
          orders-script (uss/generate-frame-script "source__orders" test-frames dependency-graph hook-map)]
      (is (string? orders-script))
      (is (.contains orders-script "Adding frame__source__orders"))
      (is (.contains orders-script "[bridge__source__orders]:"))
      (is (.contains orders-script "Left Join([bridge__source__orders])"))
      (is (.contains orders-script "Where")))))


