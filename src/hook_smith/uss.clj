(ns hook-smith.uss
  (:require [clojure.string :as str]
            [hook-smith.utilities :as utilities]))

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
       "\tLet val__fields = '';\n"
       "\n"
       "\tFor iter__field_idx = 1 To NoOfFields('$(par__table_name)') - 1\n"
       "\t\tLet val__field_name\t\t= FieldName('$(par__table_name)', $(iter__field_idx));\n"
       "\t\tLet val__field_alias\t= '[$(val__field_name)]';\n"
       "\n"
       "\t\tIf WildMatch('$(val__field_alias)', 'pit_key__*', 'hook__*') = 0 Then\n"
       "\t\t\tLet val__field_alias\t= '[$(val__field_name)] As [$(val__field_alias) (source__products)]';\n"
       "\n"
       "\t\tEnd If\n"
       "\n"
       "\t\tIf Len('$(val__fields)') > 0 Then\n"
       "\t\t\tLet val__fields = '$(val__fields), $(val__field_alias)';\n"
       "\t\t\n"
       "\t\tElse\n"
       "\t\t\tLet val__fields = '$(val__field_alias)';\n"
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

(defn generate-peripheral [peripheral frames]
  (let [name (get peripheral :name)
        source-table (get peripheral :source_table)
        target-table (get peripheral :target_table) 
        record_valid_from (get peripheral :valid_from)
        record_valid_to (get peripheral :valid_to)
        frame (first (filter #(= (:name %) name) frames))
        hooks (get frame :hooks)
        composite-hooks (get frame :composite_hooks)
        all-hooks (concat hooks composite-hooks)
        primary-hook (get-primary-hook all-hooks)
        foreign-hooks (get-foreign-hooks hooks)]
    
    (str "Trace\n"
         "------------------------------------------------------------\n"
         "Processing " name "\n"
         "------------------------------------------------------------\n"
         ";\n"
         "\n"
         "[" name "]:\n"
         "Load\n"
         "\tHash256([" primary-hook "], [" record_valid_from "])\tAs [pit_" primary-hook "]\n"
         ",\t*\n"
         "From\n"
         "\t[" source-table "] (qvd)\n"
         ";\n"
         "\n"
         "Call add_suffix_to_field_names('" name "', 'val__fields');\n"
         "\n"
         "Rename Table [" name "] To [tmp__" name "];\n"
         "\n"
         "[" name "]:\n"
         "NoConcatenate\n"
         "Load $(val__fields) Resident [tmp__" name "];\n"
         "Drop Table [tmp__" name "];\n"
         "\n"
         "Let val__fields\t= Null();\n"
         "\n"
         "// Generate bridge\n"
         "[bridge__" name "]:\n"
         "Load\n"
         "\t'" name "'\tAs [Peripheral]\n"
         ",\t[pit_" primary-hook "]\n"
         (load-hooks foreign-hooks)
         "\n"
         ",\t[" record_valid_from " (" name ")]\n"
         ",\t[" record_valid_to " (" name ")]\n"
         "\n"
         "Resident\n"
         "\t[" name "]\n"
         ";\n"
         "\n"
         
         ;; Left join the foreign tables
         ;; Inherit hooks

         "Concatenate([_bridge])\n"
         "Load\n"
         "\t[Peripheral]\n"
         ",\tHash256(\n"
         "\t\t[Peripheral]\n"
         "\t,\t[pit_" primary-hook "]\n"
         ")\tAs [key__bridge]\n"
         ",\t[pit_" primary-hook "]\n"
         (->> foreign-hooks
              (map #(str "pit_" %))
              load-hooks)
         "\n"
         ",\t[" record_valid_from " (" name ")]\tAs [" record_valid_from "]\n"
         ",\t[" record_valid_to " (" name ")]\tAs [" record_valid_to "]\n"
         "\n"
         "Resident\n"
         "\t[bridge__" name "]\n"
         ";\n"
         "\n"
         "Drop Table [bridge__" name "];\n"
         "\n"
         "// Generate peripheral\n"
         "Store [" name "] Into '" target-table "' (qvd);\n"
         "Drop Table [" name "];\n")))

(defn generate-peripherals [config]
  (let [uss (get config :unified-star-schema)
        peripherals (get uss :peripherals)
        frames (get config :frames)]
    (map #(generate-peripheral % frames) peripherals)))

(defn generate-uss-qvs [config]
  (str generate-uss-header
       "\n"
       (str/join "\n" (generate-peripherals config))))