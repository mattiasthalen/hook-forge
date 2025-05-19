(ns hook-smith.templates)

(def generate-qlik-yaml
  {:project_name "Project Name"
   :type "qlik"

   :concepts [{:name "order"
               :type "core"}

              {:name "product"
               :type "core"}]

   :keysets [{:name "source.order"
              :concept "order"
              :source_system "source"}

             {:name "source.product"
              :concept "product"
              :source_system "source"}]

   :frames [{:name "frame__source__order_lines"
             :source_system "source"
             :source_path "lib://path/to/order_lines.qvd"
             :target_path "lib://path/to/frame.qvd"
             :hooks [{:name "hook__order"
                      :primary false
                      :concept "order"
                      :qualifier nil
                      :keyset "source.order"
                      :business_key_field "order_id"}

                     {:name "hook__product"
                      :primary false
                      :concept "product"
                      :qualifier nil
                      :keyset "source.product"
                      :business_key_field "product_id"}]

             :composite_hooks [
                               {:name "hook__order_product"
                                :primary true
                                :hooks ["hook__order" "hook__product"]}]}]})