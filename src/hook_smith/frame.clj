(ns hook-smith.frame
  (:require [clojure.string :as str]
            [hook-smith.utilities :as utilities]))

(defn generate-frame-header
  "Generate the header section for a frame in the QVS script"
  [frame]
  (let [{:keys [name source_table target_table]} frame]
    (str "Trace\n"
         "============================================================\n"
         "ORGANIZING: \n"
         "Table: " name "\n"
         "Source: " source_table "\n"
         "Target: " target_table "\n"
         "============================================================\n"
         ";\n\n"
         "[" name "]:\n"
         "Load\n")))

(defn generate-hook-field
  "Generate a QVS field definition for a hook"
  [hook]
  (let [{:keys [name keyset business_key_field expression]} hook
        field-expression (if expression
                          expression
                          (str "Text([" business_key_field "])"))]
    (str "\tIf(Not IsNull(" field-expression "), '" keyset "|' & " field-expression ")\tAs [" name "]")))

(defn generate-frame-hooks
  "Generate the hook fields section for a frame"
  [frame]
  (let [hooks (:hooks frame)
        hook-fields (map generate-hook-field hooks)]
    (str/join "\n," hook-fields)))

(defn generate-composite-hooks
  "Generate composite hook definitions"
  [frame]
  (when-let [composite-hooks (:composite_hooks frame)]
    (let [composite-sections (map (fn [composite]
                                    (let [{:keys [name hooks]} composite
                                          hook-refs (map #(str "[" % "]") hooks)]
                                      (str "\t" (str/join " & '~' & " hook-refs) "\tAs [" name "]")))
                                  composite-hooks)]
      (when (seq composite-sections)
        (str (str/join "\n," composite-sections)
             "\n,\t*"
             "\n;\n\nLoad\n")))))

(defn generate-frame-script
  "Generate the complete QVS script for a single frame"
  [frame]
  (let [header (generate-frame-header frame)
        composite-hooks-script (generate-composite-hooks frame)
        hooks (generate-frame-hooks frame)]
    (str 
     header
     (if composite-hooks-script
       composite-hooks-script
       "")
     hooks
     "\n,\t*\n\n"
     "From\n"
     "\t[" (:source_table frame) "] (qvd)\n"
     ";\n\n"
     "Store [" (:name frame) "] Into [" (:target_table frame) "] (qvd);\n"
     "Drop Table [" (:name frame) "];\n\n")))

(defn should-generate-frame?
  "Check if a frame should be generated (not skipped)"
  [frame]
  (not (:skip_generation frame)))

(defn generate-frame
  "Generate QVS script for a frame, or nil if it should be skipped"
  [frame]
  (when (should-generate-frame? frame)
    (generate-frame-script frame)))

(defn generate-qvs-script
  "Generate the complete QVS script from the frames data"
  [frames]
  (->> frames
       (keep generate-frame)
       (str/join "\n")))

(defn generate-hook-qvs
  "Main function to read YAML configuration files and generate the hook.qvs script"
  [path]
  (let [filepath (str path "/generated-hook.qvs")
        content (->> path
                     (utilities/read-yaml-files-in-directory)
                     (:frames)
                     (generate-qvs-script))]
    (utilities/safe-save filepath content true)))