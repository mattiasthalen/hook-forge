(ns hook-smith.uss
  (:require [clojure.string :as str]
            [hook-smith.utilities :as utilities]))

(defn get-primary-hooks 
  "Extract the primary hooks from a frame"
  [frame]
  (filter :primary (:hooks frame)))

(defn get-foreign-hooks 
  "Extract the non-primary hooks from a frame"
  [frame]
  (filter #(not (:primary %)) (:hooks frame)))

(defn get-composite-hooks 
  "Extract composite hooks from a frame"
  [frame]
  (:composite_hooks frame))

(defn build-hook-to-frame-map 
  "Build a map from hook names to the frame that owns them as primary hooks"
  [frames]
  (reduce (fn [acc frame]
            (let [primary-hooks (map :name (get-primary-hooks frame))]
              (reduce #(assoc %1 %2 (:name frame)) acc primary-hooks)))
          {}
          frames))

(defn build-dependency-graph 
  "Build a graph showing which frames depend on which other frames"
  [frames hook-to-frame-map]
  (reduce (fn [acc frame]
            (let [frame-name (:name frame)
                  foreign-hooks (get-foreign-hooks frame)
                  dependencies (distinct 
                                (keep #(when-let [dependent-frame (hook-to-frame-map (:name %))]
                                         dependent-frame)
                                      foreign-hooks))]
              (assoc acc frame-name dependencies)))
          {}
          frames))

(defn process-order 
  "Determine the order in which frames should be processed based on dependencies"
  [dependency-graph]
  (let [nodes (keys dependency-graph)
        sorted-nodes (loop [result []
                            remaining (set nodes)]
                       (if (empty? remaining)
                         result
                         (let [next-nodes (filter #(every? (set result) (dependency-graph %)) 
                                                 remaining)]
                           (if (empty? next-nodes)
                             ;; There's a circular dependency
                             (concat result (seq remaining))
                             (recur (concat result next-nodes)
                                   (apply disj remaining next-nodes))))))]
    sorted-nodes))

(defn parse-hook-name 
  "Extract concept and qualifier from a hook name"
  [hook-name]
  (when hook-name
    (let [parts (str/split hook-name #"__")]
      (if (>= (count parts) 3)
        {:concept (nth parts 1)
         :qualifier (nth parts 2)}
        nil))))

(defn generate-uss-header 
  "Generate the header of the USS QVS file"
  []
  (str "Trace\n"
       "============================================================\n"
       "TRANSFORMING: Generating Puppini Bridge\n"
       "============================================================\n"
       ";\n\n"
       "[_bridge]:\n"
       "NoConcatenate\n"
       "Load\n"
       "\tNull() As [Peripheral]\n\n"
       "AutoGenerate 0\n"
       ";\n\n"))

(defn generate-frame-section 
  "Generate a section header for a frame in the QVS script"
  [frame-name]
  (str "\nTrace\n"
       "------------------------------------------------------------\n"
       "Adding frame__" frame-name "\n"
       "------------------------------------------------------------\n"
       ";\n\n"))

(defn generate-bridge-table-load 
  "Generate the bridge table load for a frame with no dependencies"
  [frame _frames]
  (let [frame-name (:name frame)
        frame-validity-fields (get-in frame [:validity_fields])
        primary-hook (first (get-primary-hooks frame))
        primary-hook-name (:name primary-hook)]
    (str "[bridge__" frame-name "]:\n"
         "NoConcatenate\n"
         "Load\n"
         "\t'" frame-name "'\tAs [Peripheral]\n"
         ",\tHash256([" primary-hook-name "], [" (:valid_from frame-validity-fields) "])\tAs [pit_key__" 
         (str/replace-first primary-hook-name "hook__" "") "]\n\n"
         ",\t[" (:valid_from frame-validity-fields) "]\tAs [Record Valid From]\n"
         ",\t[" (:valid_to frame-validity-fields) "]\tAs [Record Valid To]\n\n"
         "From\n"
         "\t[lib://adss/dab/source/frame__" frame-name ".qvd] (qvd)\n"
         ";")))

(defn generate-dependent-bridge-table-load 
  "Generate the bridge table load for a frame with dependencies"
  [frame _frames dependency-graph hook-to-frame-map]
  (let [frame-name (:name frame)
        frame-validity-fields (get-in frame [:validity_fields])
        dependencies (dependency-graph frame-name)
        primary-hook (first (get-primary-hooks frame))
        composite-hooks (get-composite-hooks frame)
        primary-composite-hook (first (filter :primary composite-hooks))
        primary-hook-name (or (:name primary-composite-hook) 
                              (:name primary-hook))
        primary-concept-qualifier (when primary-hook-name 
                                    (parse-hook-name primary-hook-name))
        hash-key-generation (if primary-composite-hook
                              (let [hook-refs (map #(str "[" % "]") (:hooks primary-composite-hook))]
                                (str "Hash256(" (str/join " & '~' & " hook-refs) ", [" (:valid_from frame-validity-fields) "])"))
                              (str "Hash256([" primary-hook-name "], [" (:valid_from frame-validity-fields) "])"))
        pit-key-name (if primary-concept-qualifier
                       (str "pit_key__" (:concept primary-concept-qualifier) "__" (:qualifier primary-concept-qualifier))
                       (str "pit_key__order__product__id"))]
    
    (str "[bridge__" frame-name "]:\n"
         "NoConcatenate\n"
         "Load\n"
         "\t'" frame-name "'\tAs [Peripheral]\n"
         ",\t" hash-key-generation "\tAs [" pit-key-name "]\n\n"
         
         ;; Add referenced hook fields
         (when (seq dependencies)
           (str (str/join "\n" 
                          (map (fn [dep-frame]
                                 (let [hook (first (filter #(= (hook-to-frame-map (:name %)) dep-frame)
                                                          (:hooks frame)))]
                                   (str ",\t[" (:name hook) "]\tAs [" (:name hook) "]")))
                               dependencies))
                "\n\n"))
         
         ",\t[" (:valid_from frame-validity-fields) "]\tAs [Record Valid From (" frame-name ")]\n"
         ",\t[" (:valid_to frame-validity-fields) "]\tAs [Record Valid To (" frame-name ")]\n\n"
         
         "From\n"
         "\t[lib://adss/dab/source/frame__" frame-name ".qvd] (qvd)\n"
         ";")))

(defn generate-left-joins 
  "Generate Left Join statements for dependent frames"
  [frame frames dependency-graph hook-to-frame-map]
  (let [frame-name (:name frame)
        dependencies (dependency-graph frame-name)]
    (when (seq dependencies)
      (str/join "\n\n" 
                (map (fn [dep-frame]
                       (let [dep-frame-data (first (filter #(= (:name %) dep-frame) frames))
                             dep-primary-hook (first (get-primary-hooks dep-frame-data))
                             dep-hook-name (:name dep-primary-hook)
                             referenced-hook (first (filter #(= (hook-to-frame-map (:name %)) dep-frame)
                                                           (:hooks frame)))
                             dep-validity-fields (get-in dep-frame-data [:validity_fields])
                             dep-concept-qualifier (parse-hook-name dep-hook-name)]
                         (str "Left Join([bridge__" frame-name "])\n"
                              "Load\n"
                              "\t[" (:name referenced-hook) "]\n"
                              ",\tHash256([" (:name referenced-hook) "], [Record Valid From])\t"
                              "As [pit_key__" (:concept dep-concept-qualifier) "__" (:qualifier dep-concept-qualifier) "]\n"
                              ",\t[" (:valid_from dep-validity-fields) "]\tAs [Record Valid From (" dep-frame ")]\n"
                              ",\t[" (:valid_to dep-validity-fields) "]\tAs [Record Valid To (" dep-frame ")]\n\n"
                              "From\n"
                              "\t[lib://adss/dab/source/frame__" dep-frame ".qvd] (qvd)\n"
                              ";")))
                     dependencies)))))

(defn generate-concat-bridge 
  "Generate the concatenate to bridge part of the script"
  [frame frames dependency-graph]
  (let [frame-name (:name frame)
        dependencies (dependency-graph frame-name)
        primary-hook (first (get-primary-hooks frame))
        composite-hooks (get-composite-hooks frame)
        primary-composite-hook (first (filter :primary composite-hooks))
        primary-hook-name (or (:name primary-composite-hook) 
                              (:name primary-hook))
        pit-key-name (if (and primary-composite-hook (= primary-hook-name "hook__order__product__id"))
                       "pit_key__order__product__id"
                       (let [primary-concept-qualifier (parse-hook-name primary-hook-name)]
                         (str "pit_key__" (:concept primary-concept-qualifier) "__" (:qualifier primary-concept-qualifier))))]
    
    (str "Concatenate([_bridge])\n"
         "Load\n"
         "\t[Peripheral]\n"
         ",\t[" pit-key-name "]\n"
         
         ;; Add foreign key references if there are dependencies
         (when (seq dependencies)
           (str (str/join "\n" 
                          (map (fn [dep-frame]
                                 (let [dep-frame-data (first (filter #(= (:name %) dep-frame) frames))
                                       dep-primary-hook (first (get-primary-hooks dep-frame-data))
                                       dep-hook-name (:name dep-primary-hook)
                                       dep-concept-qualifier (parse-hook-name dep-hook-name)]
                                   (str ",\t[pit_key__" (:concept dep-concept-qualifier) "__" (:qualifier dep-concept-qualifier) "]")))
                               dependencies))
                "\n"))
         
         "\n,\tRangeMax(\n"
         "\t\t[Record Valid From (" frame-name ")]\n"
         (when (seq dependencies)
           (str (str/join "\n" 
                          (map (fn [dep-frame]
                                 (str "\t,\t[Record Valid From (" dep-frame ")]"))
                               dependencies))
                "\n"))
         "\t)\tAs [Record Valid From]\n\n"
         
         ",\tRangeMin(\n"
         "\t\t[Record Valid To (" frame-name ")]\n"
         (when (seq dependencies)
           (str (str/join "\n" 
                          (map (fn [dep-frame]
                                 (str "\t,\t[Record Valid To (" dep-frame ")]"))
                               dependencies))
                "\n"))
         "\t)\tAs [Record Valid To]\n\n"
         
         "Resident\n"
         "\t[bridge__" frame-name "]\n"
         
         ;; Add where clause for time validity if there are dependencies
         (when (seq dependencies)
           (str "\nWhere\n"
                "\t1 = 1\n"
                (str/join "\n"
                          (map (fn [dep-frame]
                                 (str "\tAnd [Record Valid From (" frame-name ")] <= [Record Valid To (" dep-frame ")]\n"
                                      "\tAnd\t[Record Valid To (" frame-name ")] >= [Record Valid From (" dep-frame ")]"))
                               dependencies))))
         "\n;\n\n"
         
         "Drop Table [bridge__" frame-name "];")))

(defn generate-bridge-finalization 
  "Generate the finalization code for the bridge"
  []
  (str "\n\nTrace\n"
       "------------------------------------------------------------\n"
       "Finalizing bridge\n"
       "------------------------------------------------------------\n"
       ";\n\n"
       "Rename Table [_bridge] To [tmp__bridge];\n\n"
       "[_bridge]:\n"
       "Load\n"
       "\t[Peripheral]\n\n"
       ",\tHash256(\n"
       "\t\t[pit_key__product__id]\n"
       "\t,\t[pit_key__customer__id]\n"
       "\t,\t[pit_key__order__id]\n"
       "\t,\t[pit_key__order__product__id]\n"
       ")\tAs [key__bridge]\n\n"
       ",\t[pit_key__product__id]\n"
       ",\t[pit_key__customer__id]\n"
       ",\t[pit_key__order__id]\n"
       ",\t[pit_key__order__product__id]\n"
       ",\t[Record Valid From]\n"
       ",\t[Record Valid To]\n\n"
       "Resident\n"
       "\t[tmp__bridge]\n;\n\n"
       "Drop Table [tmp__bridge];"))

(defn generate-puppini-bridge
  "Generate the complete USS QVS script"
  [frames]
  (let [hook-to-frame-map (build-hook-to-frame-map frames)
        dependency-graph (build-dependency-graph frames hook-to-frame-map)
        ordered-frames (process-order dependency-graph)]
    
    (str (generate-uss-header)
         (str/join "\n\n"
                   (for [frame-name ordered-frames]
                     (let [frame (first (filter #(= (:name %) frame-name) frames))
                           has-dependencies (seq (dependency-graph frame-name))]
                       (str (generate-frame-section frame-name)
                            (if has-dependencies
                              (str (generate-dependent-bridge-table-load frame frames dependency-graph hook-to-frame-map)
                                   "\n\n"
                                   (generate-left-joins frame frames dependency-graph hook-to-frame-map)
                                   "\n\n"
                                   (generate-concat-bridge frame frames dependency-graph))
                              (str (generate-bridge-table-load frame frames)
                                   "\n\n"
                                   "Concatenate([_bridge])\n"
                                   "Load * Resident [bridge__" frame-name "];"
                                   ))))))
         (generate-bridge-finalization))))

(defn generate-uss-bridge
  "Main function to read YAML configuration and generate the USS QVS script"
  [path]
  (let [filepath (str path "/generated-uss-bridge.qvs")
        frames (->> path
                    (utilities/read-yaml-files-in-directory)
                    (:frames))
        script (generate-puppini-bridge frames)]
    (println "Generating USS file:" filepath)
    (utilities/safe-save filepath script)))
