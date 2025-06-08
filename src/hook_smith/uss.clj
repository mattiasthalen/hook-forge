(ns hook-smith.uss
  (:require [clojure.string :as str]))

(def generate-uss-header
  (str "Trace\n"
       "============================================================\n"
       "TRANSFORMING: Generating Unified Star Schema\n"
       "============================================================\n"
       ";\n"
       "\n"
       "[_bridge]:\n"
       "NoConcatenate\n"
       "Load\n"
       "\tNull() As [Peripheral]\n"
       "\n"
       "AutoGenerate 0\n"
       ";\n"
       "\n"
       "Sub add_suffix_to_field_names(par__table_name, par__var_name)\n"
       "\tLet val__fields\t= '';\n"
       "\n"
       "\tFor iter__field_idx = 1 To NoOfFields('$(par__table_name)')\n"
       "\t\tLet val__field_name\t\t= FieldName($(iter__field_idx), '$(par__table_name)');\n"
       "\t\tLet val__field_alias\t= '[$(val__field_name)]';\n"
       "\n"
       "\t\tIf WildMatch('$(val__field_name)', 'pit_*', 'hook__*') = 0 Then\n"
       "\t\t\tLet val__field_alias\t= '[$(val__field_name)] As [$(val__field_name) ($(par__table_name))]';\n"
       "\n"
       "\t\tEnd If\n"
       "\n"
       "\t\tIf Len('$(val__fields)') > 0 Then\n"
       "\t\t\tLet val__fields\t= '$(val__fields), $(val__field_alias)';\n"
       "\t\t\n"
       "\t\tElse\n"
       "\t\t\tLet val__fields\t= '$(val__field_alias)';\n"
       "\n"
       "\t\tEnd If\n"
       "\n"
       "\t\tLet val__field_name\t\t= Null();\n"
       "\t\tLet val__field_alias\t= Null();\n"
       "\n"
       "\tNext iter__field_idx\n"
       "\tLet iter__field_idx\t= Null();\n"
       "\n"
       "\tLet $(par__var_name) = '$(val__fields)';\n"
       "\n"
       "\tLet val__fields\t\t= Null();\n"
       "\tLet par__table_name\t= Null();\n"
       "\tLet par__var_name\t= Null();\n"
       "\n"
       "End Sub\n"
       "\n"
       "Sub deselect_hook_fields(par__table_name, par__var_name)\n"
       "\tLet val__fields\t= '';\n"
       "\n"
       "\tFor iter__field_idx = 1 To NoOfFields('$(par__table_name)')\n"
       "\t\tLet val__field_name\t= FieldName($(iter__field_idx), '$(par__table_name)');\n"
       "\n"
       "\t\tIf WildMatch('$(val__field_name)', 'hook__*') = 0 Then\n"
       "\t\t\tIf Len('$(val__fields)') > 0 Then\n"
       "\t\t\t\tLet val__fields\t= '$(val__fields), [$(val__field_name)]';\n"
       "\t\t\n"
       "\t\t\tElse\n"
       "\t\t\t\tLet val__fields\t= '[$(val__field_name)]';\n"
       "\n"
       "\t\t\tEnd If\n"
       "\t\tEnd If\n"
       "\n"
       "\t\tLet val__field_name\t= Null();\n"
       "\n"
       "\tNext iter__field_idx\n"
       "\tLet iter__field_idx\t= Null();\n"
       "\n"
       "\tLet $(par__var_name)\t= '$(val__fields)';\n"
       "\n"
       "\tLet val__fields\t\t= Null();\n"
       "\tLet par__table_name\t= Null();\n"
       "\tLet par__var_name\t= Null();\n"
       "\n"
       "End Sub\n"))

(defn get-primary-hook [hooks]
  (:name (first (filter #(= (:primary %) true) hooks))))

(defn get-foreign-hooks [hooks]
  (map :name (filter #(= (:primary %) false) hooks)))

(defn load-hooks [hooks]
  (if (empty? hooks)
    ""
    (str/join "\n"
              (map #(str ",\t[" % "]") hooks))))

(defn find-frame-by-primary-hook [frames hook-name]
  (first (filter (fn [frame]
                   (some #(and (= (:name %) hook-name) (:primary %))
                         (concat (:hooks frame) (:composite_hooks frame))))
                 frames)))

(defn get-dependencies
  "Recursively generates a list of maps {:name :primary-key :left-join :valid-from :valid-to} for all foreign hooks, in dependency order, avoiding cycles."
  [frames peripherals bridge-table foreign-hook-name visited]
  (let [frame (find-frame-by-primary-hook frames foreign-hook-name)
        peripheral (first (filter #(= (:name %) (get frame :name)) peripherals))]
    (if (or (nil? frame) (contains? visited (:name frame)))
      []
      (let [primary-hook (get-primary-hook (concat (:hooks frame) (:composite_hooks frame)))
            foreign-hooks (get-foreign-hooks (:hooks frame))
            qvd-path (get frame :target_table)
            valid-from (get peripheral :valid_from)
            valid-to (get peripheral :valid_to)
            valid-from-alias (str valid-from " (" (:name frame) ")")
            valid-to-alias (str valid-to " (" (:name frame) ")")
            join-script (str
                         "Left Join([" bridge-table "])\n"
                         "Load\n"
                         "\t[" primary-hook "]\n"
                         ",\t[" primary-hook "] & '~epoch.date|' & [Record Valid From]\tAs [pit_" primary-hook "]\n"
                         (load-hooks foreign-hooks) "\n"
                         ",\t[" valid-from "]\tAs [" valid-from-alias "]\n"
                         ",\t[" valid-to "]\tAs [" valid-to-alias "]\n"
                         "\nFrom\n"
                         "\t[" qvd-path "] (qvd)\n"
                         ";\n\n")
            visited' (conj visited (get frame :name))
            this-join {:name (get frame :name)
                       :primary-hook primary-hook
                       :valid-from valid-from-alias
                       :valid-to valid-to-alias
                       :left-join join-script}]
        (cons
         this-join
         (mapcat #(get-dependencies frames peripherals bridge-table % visited') foreign-hooks))))))

(defn generate-peripheral-header [name]
  (str "Trace\n"
       "------------------------------------------------------------\n"
       "Processing " name "\n"
       "------------------------------------------------------------\n"
       ";\n\n"))

(defn generate-peripheral-load [name primary-hook record_valid_from source-table]
  (str "[" name "]:\n"
       "Load\n"
       "\t[" primary-hook "] & '~epoch.date|' & [" record_valid_from "]\tAs [pit_" primary-hook "]\n"
       ",\t*\n"
       "From\n"
       "\t[" source-table "] (qvd)\n"
       ";\n\n"
       "Call add_suffix_to_field_names('" name "', 'val__field_list');\n\n"
       "Rename Table [" name "] To [tmp__" name "];\n\n"
       "[" name "]:\n"
       "NoConcatenate\n"
       "Load $(val__field_list) Resident [tmp__" name "];\n"
       "Drop Table [tmp__" name "];\n\n"
       "Let val__field_list\t= Null();\n\n"))

(defn generate-store-peripheral [name target-table]
  (str "// Generate peripheral\n"
       "Call deselect_hook_fields('" name "', 'val__peripheral_field_list');\n"
       "\n"
       "Store\n"
       "\t$(val__peripheral_field_list)\n"
       "\n"
       "From\n"
       "\t[" name "]\n"
       "\n"
       "Into\n"
       "\t[" target-table "] (qvd)\n"
       ";\n"
       "\n"
       "Drop Table [" name "];\n"
       "\n"
       "Let val__peripheral_field_list\t= Null();\n"
       "\n"))

(defn generate-bridge-where-clause [left-valid-from left-valid-to right-valid-froms right-valid-tos]
  (let [pairs (map vector right-valid-froms right-valid-tos)]
    (str "\nWhere\n"
         "\t1 = 1\n"
         (apply str
                (for [[rvf rvt] pairs]
                  (str "\n\tAnd " left-valid-from " <= " rvt "\n"
                       "\tAnd " left-valid-to " >= " rvf "\n"))))))

(defn generate-bridge-load-statement [bridge-table name primary-hook foreign-hooks record_valid_from record_valid_to]
  (str "// Generate bridge\n"
       "[" bridge-table "]:\n"
       "Load\n"
       "\t'" name "'\tAs [Peripheral]\n"
       ",\t[pit_" primary-hook "]\n"
       (load-hooks foreign-hooks) "\n"
       ",\t[" record_valid_from " (" name ")]\n"
       ",\t[" record_valid_to " (" name ")]\n"
       "\n"
       "Resident\n"
       "\t[" name "]\n"
       ";\n\n"))

(defn collect-dependency-metadata [dependencies]
  (let [primary-keys (map :primary-hook dependencies)
        pit-keys (map #(str "pit_" %) primary-keys)
        valid-froms (map :valid-from dependencies)
        valid-tos (map :valid-to dependencies)]
    {:pit-keys pit-keys
     :valid-froms valid-froms
     :valid-tos valid-tos}))

(defn generate-bridge-load
  [name primary-hook foreign-hooks record_valid_from record_valid_to frames peripherals]
  (let [bridge-table (str "bridge__" name)
        dependencies (mapcat #(get-dependencies frames peripherals bridge-table % #{}) foreign-hooks)
        left-joins (map :left-join dependencies)
        metadata (collect-dependency-metadata dependencies)]
    (assoc metadata
           :bridge-load (str (generate-bridge-load-statement 
                               bridge-table name primary-hook foreign-hooks 
                               record_valid_from record_valid_to)
                            (apply str left-joins)))))

(defn prepare-valid-from-to-ranges [valid-from-alias valid-to-alias valid-froms valid-tos]
  {:load-valid-from (if (seq valid-froms)
                      (str "\n\tRangeMax(\n"
                           "\t\t" valid-from-alias "\n\t,\t"
                           (str/join "\n\t,\t" valid-froms)
                           "\n\t)")
                      (str "\t" valid-from-alias))
   :load-valid-to (if (seq valid-tos)
                    (str "\n\tRangeMin(\n"
                         "\t\t" valid-to-alias "\n\t,\t"
                         (str/join "\n\t,\t" valid-tos)
                         "\n\t)")
                    (str "\t" valid-to-alias))
   :where-clause (when (seq valid-froms)
                   (generate-bridge-where-clause
                     valid-from-alias
                     valid-to-alias
                     valid-froms
                     valid-tos))})

(defn generate-bridge-concat [name primary-hook pit-keys valid-froms valid-tos record-valid-from record-valid-to]
  (let [valid-from-alias (str "[" record-valid-from " (" name ")]")
        valid-to-alias (str "[" record-valid-to " (" name ")]")
        valid-froms (map #(str "[" % "]") valid-froms)
        valid-tos (map #(str "[" % "]") valid-tos)
        ranges (prepare-valid-from-to-ranges valid-from-alias valid-to-alias valid-froms valid-tos)
        load-valid-from (:load-valid-from ranges)
        load-valid-to (:load-valid-to ranges)
        where-clause (:where-clause ranges)
        pit-keys-str (when (seq pit-keys)
                       (str (str/join "\n" (map #(str "\t,\t[" % "]") pit-keys)) "\n"))]
    (str "Concatenate([_bridge])\n"
         "Load\n"
         "\t[Peripheral]\n"
         ",\tHash256(\n"
         "\t\t[Peripheral]\n"
         "\t,\t[pit_" primary-hook "]\n"
         (or pit-keys-str "")
         "\t)\tAs [key__bridge]\n\n"
         ",\t[pit_" primary-hook "]\n"
         (load-hooks pit-keys) "\n"
         "," load-valid-from "\tAs [" record-valid-from "]\n"
         "," load-valid-to "\tAs [" record-valid-to "]\n"
         "\n"
         "Resident\n"
         "\t[bridge__" name "]\n"
         where-clause
         ";\n\n"
         "Drop Table [bridge__" name "];\n\n")))

(defn extract-frame-data [peripheral frames]
  (let [name (:name peripheral)
        frame (first (filter #(= (:name %) name) frames))
        hooks (:hooks frame)
        composite-hooks (:composite_hooks frame)
        all-hooks (concat hooks composite-hooks)
        primary-hook (get-primary-hook all-hooks)
        foreign-hooks (get-foreign-hooks hooks)]
    {:frame frame
     :hooks hooks
     :composite-hooks composite-hooks
     :all-hooks all-hooks
     :primary-hook primary-hook
     :foreign-hooks foreign-hooks}))

(defn generate-peripheral [peripheral frames peripherals]
  (let [name (:name peripheral)
        source-table (:source_table peripheral)
        target-table (:target_table peripheral)
        record_valid_from (:valid_from peripheral)
        record_valid_to (:valid_to peripheral)
        frame-data (extract-frame-data peripheral frames)
        primary-hook (:primary-hook frame-data)
        foreign-hooks (:foreign-hooks frame-data)
        bridge-load-map (generate-bridge-load 
                          name primary-hook foreign-hooks 
                          record_valid_from record_valid_to 
                          frames peripherals)
        pit-keys (:pit-keys bridge-load-map)
        valid-froms (:valid-froms bridge-load-map)
        valid-tos (:valid-tos bridge-load-map)
        bridge-loads (:bridge-load bridge-load-map)]
    (str
     (generate-peripheral-header name)
     (generate-peripheral-load name primary-hook record_valid_from source-table)
     bridge-loads
     (generate-bridge-concat name primary-hook pit-keys valid-froms valid-tos record_valid_from record_valid_to)
     (generate-store-peripheral name target-table))))

(defn generate-peripherals [config]
  (let [uss (get config :unified-star-schema)
        peripherals (get uss :peripherals)
        frames (get config :frames)]
    (map #(generate-peripheral % frames peripherals) peripherals)))

(def generate-event-header
  (str
   "Trace\n"
   "------------------------------------------------------------\n"
   "Adding events to bridge\n"
   "------------------------------------------------------------\n"
   ";\n\n"
   "[events]:\n"
   "Load\n"
   "\tNull()\tAs [key__bridge]"
   ",\tNull()\tAs [hook__epoch__date]\n"
   "\n"
   "AutoGenerate 0\n"
   ";"))

(defn generate-event-load [source-table primary-key event record-valid-from]
  (let [event-alias (get event :name)
        event-field (get event :field)
        event-load (if (nil? event-field) event-alias event-field)]
    (str "["event-alias"]:\n"
         "NoConcatenate\n"
         "Load\n"
         "\t[key__bridge]\n"
         ",\t[pit_" primary-key "]\n"
         "\n"
         "Resident\n"
         "\t[_bridge]\n"
          "\n"
          "Where\n"
          "\t1 = 1\n"
          "\tAnd Len([pit_" primary-key "]) > 0\n"
         ";\n"
         "\n"
         "Left Join(["event-alias"])\n"
         "Load\n"
         "[" primary-key "] & '~epoch.date|' & [" record-valid-from "]\tAs [pit_" primary-key "]\n"
         ",\t1\tAs [Event: " event-alias "]\n"
         ",\t'epoch.date|' & [" event-load "]\tAs [hook__epoch__date]\n"
         "\nFrom\n"
         "\t[" source-table "] (qvd)\n"
         "\n"
         "Where\n"
         "\t1 = 1\n"
         "\tAnd Len([" event-load "]) > 0\n"
         ";\n"
         "\n"
         "Concatenate([events])\n"
         "Load * Resident [" event-alias "];\n"
         "Drop Table [" event-alias "];\n")))

(defn generate-event-loads [peripheral frames]
  (let [name (get peripheral :name)
        source-table (get peripheral :source_table)
        record-valid-from (get peripheral :valid_from)
        events (get peripheral :events)
        frame (first (filter #(= (:name %) name) frames))
        primary-hook (get-primary-hook (concat (:hooks frame) (:composite_hooks frame)))]
    (str/join "\n"
              (map #(generate-event-load source-table primary-hook % record-valid-from) events))))

(defn generate-event-bridge [peripherals]
  (let [events (mapcat #(get % :events) peripherals)
        event-names (map #(get % :name) events)]
    (str "Left Join([events])\n"
         "Load * Resident [_bridge];\n"
         "\n"
         "Left Join([_bridge])\n"
         "Load\n"
         "\t[key__bridge]\n"
         ",\t[hook__epoch__date]\n"
         ",\t" (str/join "\n,\t" (map #(str "Count([Event: " % "])\tAs [Event: " % "]") event-names))
         "\n\n"
         "Resident\n"
         "\t[events]\n"
         "\n"
         "Group By\n"
         "\t[key__bridge]\n"
         ",\t[hook__epoch__date]\n"
         ";"
         "\n\n"
         "Drop Table [events];")))

(defn generate-event-section [config]
  (let [uss (get config :unified-star-schema)
        frames (get config :frames)
        peripherals (get uss :peripherals)]
    (str generate-event-header
         "\n"
         (str/join "\n"
                   (map #(generate-event-loads % frames) peripherals))
         "\n"
         (generate-event-bridge peripherals))))

(defn generate-uss-footer [config]
  (let [uss (get config :unified-star-schema)
        bridge-path (get uss :bridge_path)]
    (str "Store [_bridge] Into [" bridge-path "] (qvd);\n"
         "Drop Table [_bridge];")))

(defn generate-uss-qvs [config]
  (str generate-uss-header
       "\n"
       (str/join "\n" (generate-peripherals config))
       "\n"
       (generate-event-section config)
       "\n\n"
       (generate-uss-footer config)))