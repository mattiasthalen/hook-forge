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
  [{:name "frame__source__products"
    :source_system "source"
    :source_table "das.raw__source__products"
    :source_path "lib://adss/das/source/raw__source__products.qvd"
    :target_path "lib://adss/dab/source/frame__source__products.qvd"
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
   {:name "frame__source__customers"
    :source_system "source"
    :source_table "das.raw__source__customers"
    :source_path "lib://adss/das/source/raw__source__customers.qvd"
    :target_path "lib://adss/dab/source/frame__source__customers.qvd"
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
   {:name "frame__source__orders"
    :source_system "source"
    :source_table "das.raw__source__orders"
    :source_path "lib://adss/das/source/raw__source__orders.qvd"
    :target_path "lib://adss/dab/source/frame__source__orders.qvd"
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
   {:name "frame__source__order_lines"
    :source_system "source"
    :source_table "das.raw__source__order_lines"
    :source_path "lib://adss/das/source/raw__source__order_lines.qvd"
    :target_path "lib://adss/dab/source/frame__source__order_lines.qvd"
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
                       :hooks ["hook__order__id" "hook__product__id"]}]}])

(defn generate-blueprint-file
  "Generate a single blueprint file from blueprint data"
  [path [name data]]
  (let [filepath (str path "/" name ".yaml")]
    
    (->> data
         (utilities/convert-map-to-yaml)
         (utilities/separate-blocks-on-dedent)
         (utilities/safe-save filepath))))