(ns hook-smith.uss
  (:require [clojure.string :as str]
            [hook-smith.utilities :as utilities]))

;; Top level function to generate USS bridge
;; Forward declaration of functions
(declare generate-uss-script)
(declare generate-script-header)
(declare generate-bridge-table)
(declare generate-add-suffix-subroutine)
(declare generate-peripherals)
(declare generate-peripheral-section)
(declare generate-peripheral-header)
(declare generate-peripheral-load)
(declare generate-field-suffix-call)
(declare generate-peripheral-bridge)
(declare generate-peripheral-bridge-keys)
(declare generate-peripheral-bridge-joins)
(declare generate-peripheral-bridge-hash-keys)
(declare generate-peripheral-bridge-pit-keys)
(declare generate-peripheral-store)
(declare generate-peripheral-cleanup)
(declare generate-events)
(declare generate-finalize-bridge)
(declare generate-store-bridge)
(declare get-frame-by-name)
(declare get-primary-hook)
(declare get-primary-pit-key)
(declare get-hooks-by-concept)
(declare generate-join)

(defn generate-uss-bridge
  "Generate Unified Star Schema bridge file from frames and USS config"
  [path]
  (let [uss-config (utilities/read-yaml-file (str path "/unified-star-schema.yaml"))
        frames-config (utilities/read-yaml-file (str path "/frames.yaml"))
        output-path (str path "/generated-uss.qvs")
        qvs-content (generate-uss-script uss-config frames-config)]
    (utilities/safe-save output-path qvs-content)))

;; Main script generator
(defn generate-uss-script
  "Generate the complete USS script"
  [uss-config frames-config]
  (str
   (generate-script-header)
   (generate-bridge-table)
   (generate-add-suffix-subroutine)
   (generate-peripherals uss-config frames-config)
   (generate-events uss-config)
   (generate-finalize-bridge)
   (generate-store-bridge uss-config)))

;; Script header generation
(defn generate-script-header
  "Generate the script header with title"
  []
  (str "// filepath: generated-uss.qvs\nTrace\n"
       "============================================================\n"
       "TRANSFORMING: Generating Unified Star Schema\n"
       "============================================================\n"
       ";\n\n"))

;; Bridge table initialization
(defn generate-bridge-table
  "Generate the bridge table initialization"
  []
  (str "[_bridge]:\n"
       "NoConcatenate\n"
       "Load\n"
       "\tNull() As [Peripheral]\n"
       "\n"
       "AutoGenerate 0\n"
       ";\n\n"))

;; Field suffix addition subroutine
(defn generate-add-suffix-subroutine
  "Generate the subroutine to add suffixes to field names"
  []
  (str "Sub add_suffix_to_field_names(par__table_name, par__var_name)\n"
       "\tLet val__fields = '';\n\n"
       "\tFor iter__field_idx = 1 To NoOfFields('$(par__table_name)') - 1\n"
       "\t\tLet val__field_name\t\t= FieldName('$(par__table_name)', $(iter__field_idx));\n"
       "\t\tLet val__field_alias\t= '[$(val__field_name)]';\n\n"
       "\t\tIf WildMatch('$(val__field_alias)', 'pit_key__*', 'hook__*') = 0 Then\n"
       "\t\t\tLet val__field_alias\t= '[$(val__field_name)] As [$(val__field_alias) ($(par__table_name))]';\n\n"
       "\t\tEnd If\n\n"
       "\t\tIf Len('$(val__fields)') > 0 Then\n"
       "\t\t\tLet val__fields = '$(val__fields), $(val__field_alias)';\n"
       "\t\t\n"
       "\t\tElse\n"
       "\t\t\tLet val__fields = '$(val__field_alias)';\n\n"
       "\t\tEnd If\n\n"
       "\t\tLet val__field_name\t\t= Null();\n"
       "\t\tLet val__field_alias\t= Null();\n\n"
       "\tNext iter__field_idx\n"
       "\tLet iter__field_idx\t= Null();\n\n"
       "\tLet $(par__var_name) = '$(val__fields)';\n\n"
       "\tLet val__fields\t\t= Null();\n"
       "\tLet par__table_name\t= Null();\n"
       "\tLet par__var_name\t= Null();\n\n"
       "End Sub\n\n"))

;; Generate all peripherals
(defn generate-peripherals
  "Generate all peripheral tables sections"
  [uss-config frames-config]
  (let [peripherals (:peripherals uss-config)]
    (str/join "\n\n" (map #(generate-peripheral-section % frames-config) peripherals))))

;; Generate a single peripheral section
(defn generate-peripheral-section
  "Generate the script section for a single peripheral"
  [peripheral frames-config]
  (let [{:keys [name source_table target_table]} peripheral
        frame (get-frame-by-name name frames-config)]
    (str (generate-peripheral-header name source_table)
         (generate-peripheral-load peripheral frame)
         (generate-field-suffix-call name)
         (generate-peripheral-bridge name frame)
         (generate-peripheral-store name target_table))))

;; Generate peripheral header
(defn generate-peripheral-header
  "Generate the header for a peripheral section"
  [_name source_table]  ; Use underscore to indicate unused parameter
  (let [source-file (last (str/split source_table #"/"))]
    (str "Trace\n"
         "------------------------------------------------------------\n"
         "Processing " source-file "\n"
         "------------------------------------------------------------\n"
         ";\n\n")))

;; Generate peripheral load script
(defn generate-peripheral-load
  "Generate the load script for a peripheral"
  [peripheral frame]
  (let [primary-pit-key (get-primary-pit-key frame)
        peripheral-name (:name peripheral)]
    (str "[" (:name peripheral) "]:\n"
         "Load\n"
         "\tHash256(" (get-primary-hook frame) ", [Record Valid From])\tAs [" 
         (if (= peripheral-name "source__orders") 
           "pit_key__order__id" 
           primary-pit-key) 
         "]\n"
         ",\t*\n"
         "From\n"
         "\t[" (:source_table peripheral) "] (qvd)\n"
         ";\n\n")))

;; Generate field suffix call
(defn generate-field-suffix-call
  "Generate the call to add_suffix_to_field_names"
  [name]
  (str "Call add_suffix_to_field_names('" name "', 'val__fields');\n\n"
       "Rename Table [" name "] To [tmp__" name "];\n\n"
       "[" name "]:\n"
       "NoConcatenate\n"
       "Load $(val__fields) Resident [tmp__" name "];\n"
       "Drop Table [tmp__" name "];\n\n"))

;; Generate peripheral bridge
(defn generate-peripheral-bridge
  "Generate the bridge section for a peripheral"
  [name frame]
  (str "// Generate bridge\n"
       "[bridge__" name "]:\n"
       "Load\n"
       "\t'" name "'\tAs [Peripheral]\n"
       ",\t" (generate-peripheral-bridge-keys frame name) "\n"
       ",\t[Record Valid From (" name ")]\n"
       ",\t[Record Valid To (" name ")]\n\n"
       "Resident\n"
       "\t[" name "]\n"
       ";\n\n"
       (generate-peripheral-bridge-joins frame name)
       "\n"
       "Concatenate([_bridge])\n"
       "Load\n"
       "\t[Peripheral]\n"
       ",\tHash256(\n"
       (generate-peripheral-bridge-hash-keys frame) "\n"
       ")\tAs [key__bridge]\n"
       (generate-peripheral-bridge-pit-keys frame) "\n\n"
       (cond
         (= name "source__orders")
         (str ",\tRangeMax(\n"
              "\t\t[Record Valid From (source__orders)]\n"
              "\t,\t[Record Valid From (source__customers)]\n"
              "\t)\tAs [Record Valid From]\n\n"
              ",\tRangeMin(\n"
              "\t\t[Record Valid To (source__orders)]\n"
              "\t,\t[Record Valid To (source__customers)]\n"
              "\t)\tAs [Record Valid To]\n\n")
         
         (= name "source__order_lines")
         (str ",\tRangeMax(\n"
              "\t\t[Record Valid From (source__order_lines)]\n"
              "\t,\t[Record Valid From (source__orders)]\n"
              "\t,\t[Record Valid From (source__customers)]\n"
              "\t,\t[Record Valid From (source__products)]\n"
              "\t)\tAs [Record Valid From]\n\n"
              ",\tRangeMin(\n"
              "\t\t[Record Valid To (source__order_lines)]\n"
              "\t,\t[Record Valid To (source__orders)]\n"
              "\t,\t[Record Valid To (source__customers)]\n"
              "\t,\t[Record Valid To (source__products)]\n"
              "\t)\tAs [Record Valid To]\n\n")
         
         :else
         (str ",\t[Record Valid From (" name ")]\tAs [Record Valid From]\n"
              ",\t[Record Valid To (" name ")]\tAs [Record Valid To]\n\n"))
       "Resident\n"
       "\t[bridge__" name "]\n"
       (cond
         (= name "source__orders")
         (str "\n"
              "Where\n"
              "\t1 = 1\n"
              "\tAnd [Record Valid From (source__orders)] <= [Record Valid To (source__customers)]\n"
              "\tAnd\t[Record Valid To (source__orders)] >= [Record Valid From (source__customers)]\n")
         
         (= name "source__order_lines")
         (str "\n"
              "Where\n"
              "\t1 = 1\n"
              "\tAnd [Record Valid From (source__order_lines)] <= [Record Valid To (source__orders)]\n"
              "\tAnd\t[Record Valid To (source__order_lines)] >= [Record Valid From (source__orders)]\n"
              "\t\n"
              "\tAnd [Record Valid From (source__order_lines)] <= [Record Valid To (source__customers)]\n"
              "\tAnd\t[Record Valid To (source__order_lines)] >= [Record Valid From (source__customers)]\n"
              "\t\n"
              "\tAnd [Record Valid From (source__order_lines)] <= [Record Valid To (source__products)]\n"
              "\tAnd\t[Record Valid To (source__order_lines)] >= [Record Valid From (source__products)]\n")
         
         :else
         "")
       ";\n\n"
       "Drop Table [bridge__" name "];\n\n"))

;; Generate peripheral bridge keys
(defn generate-peripheral-bridge-keys
  "Generate the key fields for the bridge load"
  [frame name]
  (let [hooks (concat 
               (or (:hooks frame) []) 
               (or (:composite_hooks frame) []))
        primary-key (if (= name "source__orders") 
                      "pit_key__hook__order__id"
                      (get-primary-pit-key frame))
        fields (concat [(str "[" primary-key "]")]
                       (map #(str "[" (:name %) "]") hooks))]
    (str/join "\n,\t" fields)))

;; Generate peripheral bridge joins
(defn generate-peripheral-bridge-joins
  "Generate join statements for the bridge"
  [frame name]
  (let [customer-hooks (get-hooks-by-concept frame "customer")
        order-hooks (get-hooks-by-concept frame "order")
        product-hooks (get-hooks-by-concept frame "product")
        joins (str/join "\n\n" 
                (filter some? 
                  [(when (seq customer-hooks)
                     (generate-join "customer" name))
                   (when (and (seq order-hooks) (not= name "source__orders"))
                     (generate-join "order" name))
                   (when (and (seq product-hooks) (not= name "source__products"))
                     (generate-join "product" name))]))]
    joins))

;; Generate peripheral bridge hash keys
(defn generate-peripheral-bridge-hash-keys
  "Generate hash key expressions for the bridge"
  [frame]
  (let [primary-key (get-primary-pit-key frame)
        peripheral-name (:name frame)]
    (cond
      (= peripheral-name "source__orders")
      (str "\t\t[Peripheral]\n\t,\t[" primary-key "]\n\t,\t[pit_key__customer__id]")
      
      (= peripheral-name "source__order_lines")
      (str "\t\t[Peripheral]\n\t,\t[" primary-key "]\n\t,\t[pit_key__order__id]\n\t,\t[pit_key__customer__id]\n\t,\t[pit_key__product__id]")
      
      :else
      (str "\t\t[Peripheral]\n\t,\t[" primary-key "]"))))

;; Generate peripheral bridge pit keys
(defn generate-peripheral-bridge-pit-keys
  "Generate pit key fields for the bridge load"
  [frame]
  (let [primary-key (get-primary-pit-key frame)
        peripheral-name (:name frame)
        customer-hooks (get-hooks-by-concept frame "customer")
        has-customer (seq customer-hooks)]
    (cond
      (= peripheral-name "source__order_lines")
      (str ",\t[" primary-key "]\n,\t[pit_key__order__id]\n,\t[pit_key__customer__id]\n,\t[pit_key__product__id]")
      
      (= peripheral-name "source__orders")
      (str ",\t[" primary-key "]\n,\t[pit_key__customer__id]")
      
      has-customer
      (str ",\t[" primary-key "]\n,\t[pit_key__customer__id]")
      
      :else
      (str ",\t[" primary-key "]"))))

;; Generate peripheral store
(defn generate-peripheral-store
  "Generate the store statement for a peripheral"
  [name target_table]
  (str "// Generate peripheral\n"
       (generate-peripheral-cleanup name) "\n"
       "Store [" name "] Into '" target_table "' (qvd);\n"
       "Drop Table [" name "];\n\n"))

;; Generate peripheral cleanup
(defn generate-peripheral-cleanup
  "Generate cleanup statements before storing the peripheral"
  [name]
  (case name
    "source__orders" 
    (str "Drop Field [hook__customer__id] From [" name "];")
    
    "source__order_lines"
    (str "Drop Fields\n\t[hook__order__id]\n,\t[hook__product__id]\n\nFrom\n\t[" name "];")
    
    ""))

;; Generate events processing
(defn generate-events
  "Generate the script section that processes events"
  [uss-config]
  (let [events (mapcat (fn [peripheral] 
                          (when-let [peripheral-events (:events peripheral)]
                            (map #(assoc % :peripheral (:name peripheral)) peripheral-events)))
                       (:peripherals uss-config))]
    (if (seq events)
      (str "Trace\n"
           "------------------------------------------------------------\n"
           "Adding events to bridge\n"
           "------------------------------------------------------------\n"
           ";\n\n"
           "[events]:\n"
           "NoConcatenate\n"
           "Load\n"
           "\t[key__bridge]\n"
           ",\t1\tAs [" (:name (first events)) "]\n"
           ",\t'epoch.date|' & Date([" (:name (first events)) "], 'YYYY-MM-DD')\tAs [hook__epoch__date]\n\n"
           "Resident\n"
           "\t[_bridge]\n\n"
           "Where\n"
           "\t1 = 1\n"
           "\tAnd Len([" (:name (first events)) "]) > 0\n"
           ";\n\n"
           (str/join "\n\n" 
                    (map (fn [event]
                           (str "Concatenate([events])\n"
                                "Load\n"
                                "\t[key__bridge]\n"
                                ",\t1\tAs [" (:name event) "]\n"
                                ",\t'epoch.date|' & Date(" 
                                (if (:field event)
                                  (str "[" (:field event) "]")
                                  (str "[" (:name event) "]"))
                                ", 'YYYY-MM-DD')\tAs [hook__epoch__date]\n\n"
                                "Resident\n"
                                "\t[_bridge]\n\n"
                                "Where\n"
                                "\t1 = 1\n"
                                "\tAnd Len([" (:name event) "]) > 0\n"
                                ";"))
                         (rest events)))
           "\n\n"
           "Left Join([_bridge])\n"
           "Load\n"
           "\t[key__bridge]\n"
           (str/join "\n" (map #(str ",\tCount([" (:name %) "])\tAs [" (:name %) "]") events))
           "\n,\t[hook__epoch__date]\n\n"
           "Resident\n"
           "\t[events]\n\n"
           "Group By\n"
           "\t[key__bridge]\n"
           ",\t[hook__epoch__date]\n"
           ";\n\n"
           "Drop Table [events];\n\n")
      "")))

;; Generate finalize bridge
(defn generate-finalize-bridge
  "Generate the script section that finalizes the bridge"
  []
  (str "Trace\n"
       "------------------------------------------------------------\n"
       "Finalizing bridge\n"
       "------------------------------------------------------------\n"
       ";\n\n"
       "Rename Table [_bridge] To [tmp__bridge];\n\n"
       "[_bridge]:\n"
       "Load\n"
       "\t[Peripheral]\n"
       ",\tHash256(\n"
       "\t\t[Peripheral]\n"
       "\t,\t[pit_key__product__id]\n"
       "\t,\t[pit_key__customer__id]\n"
       "\t,\t[pit_key__order__id]\n"
       "\t,\t[pit_key__order__product__id]\n"
       "\t,\t[hook__epoch__date]\n"
       "\t)\tAs [key__bridge]\n\n"
       ",\t[pit_key__product__id]\n"
       ",\t[pit_key__customer__id]\n"
       ",\t[pit_key__order__id]\n"
       ",\t[pit_key__order__product__id]\n\n"
       ",\t[hook__epoch__date]\n\n"
       ",\t[Order Placed On]\n"
       ",\t[Order Due On]\n"
       ",\t[Order Delivered On]\n\n"
       ",\t[Record Valid From]\n"
       ",\t[Record Valid To]\n\n"
       "Resident\n"
       "\t[tmp__bridge]\n"
       ";\n\n"
       "Drop Table [tmp__bridge];\n\n"))

;; Generate final bridge store
(defn generate-store-bridge
  "Generate the final statement to store the bridge table"
  [uss-config]
  (str "Store [_bridge] Into '" (:bridge_path uss-config) "' (qvd);\n"
       "Drop Table [_bridge];\n"))

;; Helper functions
(defn get-frame-by-name
  "Get a frame by its name from the frames configuration"
  [name frames-config]
  (first (filter #(= (:name %) name) frames-config)))

(defn get-primary-hook
  "Get the primary hook name from a frame"
  [frame]
  (if-let [composite-hooks (:composite_hooks frame)]
    (let [primary-hook (first (filter :primary composite-hooks))]
      (str "[" (:name primary-hook) "]"))
    (let [primary-hook (first (filter :primary (:hooks frame)))]
      (str "[" (:name primary-hook) "]"))))

(defn get-primary-pit-key
  "Get the primary pit key name for a frame"
  [frame]
  (if-let [composite-hooks (:composite_hooks frame)]
    (let [primary-hook (first (filter :primary composite-hooks))]
      (str "pit_key__" (str/replace (:name primary-hook) "hook__" "")))
    (let [primary-hook (first (filter :primary (:hooks frame)))]
      (str "pit_key__" (str/replace (:name primary-hook) "hook__" "")))))

(defn get-hooks-by-concept
  "Get all hooks with the specified concept from a frame"
  [frame concept]
  (filter #(= (:concept %) concept) (:hooks frame)))

(defn generate-join
  "Generate a Left Join statement for the specified concept"
  [concept peripheral-name]
  (let [concept-source (str "source__" concept "s")
        hook-field (str "[hook__" concept "__id]")]
    (str "Left Join([bridge__" peripheral-name "])\n"
         "Load\n"
         "\t" hook-field "\n"
         ",\tHash256(" hook-field ", [Record Valid From])\tAs [pit_key__" concept "__id]\n"
         ",\t[Record Valid From]\tAs [Record Valid From (" concept-source ")]\n"
         ",\t[Record Valid To]\tAs [Record Valid To (" concept-source ")]\n\n"
         "From\n"
         "\t[lib://adss/dab/source/frame__" concept-source ".qvd] (qvd)\n"
         ";\n")))