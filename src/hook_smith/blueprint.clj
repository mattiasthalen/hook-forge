(ns hook-smith.blueprint
  (:require [hook-smith.utilities :as utilities]))

(def concepts-blueprint
  "Generates a concepts block for the blueprint."
  [{:name "product"
    :type "core"}
   {:name "customer"
    :type "core"}
   {:name "order"
    :type "core"}])

(def keysets-blueprint
  "Generates a keysets block for the blueprint."
   [{:name "source.product.id"
     :concept "product"
     :qualifier "id"
     :source_system "source"}
    {:name "source.product.name"
     :concept "product"
     :qualifier "name"
     :source_system "source"}
    {:name "source.customer.id"
     :concept "customer"
     :qualifier "id"
     :source_system "source"}
    {:name "source.customer.name"
     :concept "customer"
     :qualifier "name"
     :source_system "source"}
    {:name "source.order.id"
     :concept "order"
     :qualifier "id"
     :source_system "source"}
    {:name "source.order.order_number"
     :concept "order"
     :qualifier "order_number"
     :source_system "source"}])

(def frames-blueprint
  "Generates a frames block for the blueprint."
  [{:name "source__products"
    :source_system "source"
    :source_table "lib://adss/das/source/raw__source__products.qvd"
    :target_table "lib://adss/dab/source/frame__source__products.qvd"
    :hooks [{:name "hook__product__id"
             :primary true
             :concept "product"
             :qualifier "id"
             :keyset "source.product.id"
             :business_key_field "id"}
            {:name "hook__product__name"
             :primary false
             :concept "product"
             :qualifier "name"
             :keyset "source.product.name"
             :business_key_field "name"}
            ]}
   {:name "source__customers"
    :source_system "source"
    :source_table "lib://adss/das/source/raw__source__customers.qvd"
    :target_table "lib://adss/dab/source/frame__source__customers.qvd"
    :hooks [{:name "hook__customer__id"
             :primary true
             :concept "customer"
             :qualifier "id"
             :keyset "source.customer.id"
             :business_key_field "id"}
            {:name "hook__customer__name"
             :primary false
             :concept "customer"
             :qualifier "name"
             :keyset "source.customer.name"
             :business_key_field "name"}]}
   {:name "source__orders"
    :source_system "source"
    :source_table "lib://adss/das/source/raw__source__orders.qvd"
    :target_table "lib://adss/dab/source/frame__source__orders.qvd"
    :hooks [{:name "hook__order__id"
             :primary true
             :concept "order"
             :qualifier "id"
             :keyset "source.order.id"
             :business_key_field "id"}
            {:name "hook__order__order_number"
             :primary false
             :concept "order"
             :qualifier "order_number"
             :keyset "source.order.order_number"
             :business_key_field "order_number"}
            {:name "hook__customer__id"
             :primary false
             :concept "customer"
             :qualifier "id"
             :keyset "source.customer.id"
             :business_key_field "customer_id"}]}
   {:name "source__order_lines"
    :source_system "source"
    :source_table "lib://adss/das/source/raw__source__order_lines.qvd"
    :target_table "lib://adss/dab/source/frame__source__order_lines.qvd"
    :hooks [{:name "hook__order__id"
             :primary false
             :concept "order"
             :qualifier "id"
             :keyset "source.order.id"
             :business_key_field "order_id"}
            {:name "hook__product__id"
             :primary false
             :concept "product"
             :qualifier "id"
             :keyset "source.product.id"
             :business_key_field "product_id"}]
    :composite_hooks [{:name "hook__order__product__id"
                       :primary true
                       :hooks ["hook__order__id" "hook__product__id"]}]}
   {:name "source__customer_orders"
    :skip_generation true
    :source_system "source"
    :source_table ["lib://adss/das/source/frame__source__customers.qvd" 
                   "lib://adss/das/source/frame__source__orders.qvd"]
    :target_table "lib://adss/dab/source/frame__source__customer_orders.qvd"
    :hooks [{:name "hook__customer__id"
             :primary false
             :concept "customer"
             :qualifier "id"
             :keyset "source.customer.id"
             :business_key_field "id"}
            {:name "hook__order__id"
             :primary false
             :concept "order"
             :qualifier "id"
             :keyset "source.order.id"
             :business_key_field "order_id"}]
    :composite_hooks [{:name "hook__customer__order__id"
                       :primary true
                       :hooks ["hook__customer__id" "hook__order__id"]}]}])

(def uss-blueprint
  {:bridge_path "lib://adss/dar/_bridge.qvd"
   :peripherals [{:name "source__products"
                 :source_table "lib://adss/dab/source/frame__source__products.qvd"
                 :target_table "lib://adss/dar/source__products.qvd"
                 :valid_from "Record Valid From"
                 :valid_to "Record Valid To"}
                {:name "source__customers"
                 :source_table "lib://adss/dab/source/frame__source__customers.qvd"
                 :target_table "lib://adss/dar/source__customers.qvd"
                 :valid_from "Record Valid From"
                 :valid_to "Record Valid To"}
                {:name "source__orders"
                 :source_table "lib://adss/dab/source/frame__source__orders.qvd"
                 :target_table "lib://adss/dar/source__orders.qvd"
                 :valid_from "Record Valid From"
                 :valid_to "Record Valid To"
                 :events [{:name "Order Placed On"}
                          {:name "Order Due On"}
                          {:name "Order Delivered On" 
                           :field "order_delivered_on"}]}
                {:name "source__order_lines"
                 :source_table "lib://adss/dab/source/frame__source__order_lines.qvd"
                 :target_table "lib://adss/dar/source__order_lines.qvd"
                 :valid_from "Record Valid From"
                 :valid_to "Record Valid To"}
                {:name "source__customer_orders"
                 :source_table "lib://adss/dab/source/frame__source__customer_orders.qvd"
                 :target_table "lib://adss/dar/source__customer_orders.qvd"
                 :valid_from "Record Valid From"
                 :valid_to "Record Valid To"}]})

(defn generate-blueprint-file
  "Generate a single blueprint file from blueprint data"
  [path [name data]]
  (let [filepath (str path "/" name ".yaml")]
    
    (->> data
         (utilities/convert-map-to-yaml)
         (utilities/separate-blocks-on-dedent)
         (utilities/safe-save filepath))))