(ns hook-smith.documentation
  (:require [clojure.string :as string]))

(defn get-primary-hook [frame]
  (let [hooks (get frame :hooks [])
        composite-hooks (get frame :composite_hooks [])
        primary-hook (first (filter #(true? (:primary %)) hooks))
        primary-composite-hook (first (filter #(true? (:primary %)) composite-hooks))]
    (or (:name primary-composite-hook) (:name primary-hook))))

(defn get-foreign-key-hooks [frame]
  (let [hooks (get frame :hooks [])
        non-primary-hooks (filter #(not (true? (:primary %))) hooks)]
    (map :name non-primary-hooks)))

(defn generate-hook-entity-block [frame]
  (let [frame-name (:name frame)
        primary-hook (get-primary-hook frame)
        foreign-keys (get-foreign-key-hooks frame)]
    (str "    frame__" frame-name "(\"
        **FRAME__" (string/upper-case frame-name) "**
        **Primary Key:**
        " primary-hook "
        &nbsp;
        **Foreign Keys:**
        " (string/join "\n        " foreign-keys) "
    \")")))

(defn generate-hook-section [frames]
  (let [entities (string/join "\n\n" (map generate-hook-entity-block frames))]
    (str "## Hook\n```mermaid\nflowchart LR\n    %% Entities\n"
         entities
         "\n\n    %% Relations\n"
         "    frame__source__order_lines -- hook__order__id --> frame__source__orders\n"
         "    frame__source__order_lines -- hook__order__product__id ---> frame__source__products\n\n"
         "    frame__source__orders -- hook__customer__id --> frame__source__customers\n\n"
         "```")))

(defn generate-bridge-entity-block []
  (str "    bridge(\"
        **_BRIDGE**
        **Primary Key:**
        key__bridge
        &nbsp;
        **Foreign Keys:**
        pit_hook__product__id
        pit_hook__customer__id
        pit_hook__order__id
        pit_hook__order__product__id
        &nbsp;
        **Dimensions:**
        Peripheral
        Record Valid From
        Record Valid To
    \")"))

(defn generate-peripheral-entity-block [peripheral]
  (let [name (:name peripheral)
        prim-key (str "pit_hook__" 
                      (if (= name "source__order_lines") 
                        "order__product" 
                        (string/replace (second (string/split name #"__")) #"s$" ""))
                      "__id")]
    (str "    " name "(\"
        **" (string/upper-case name) "**
        **Primary Key:**
        " prim-key "
        &nbsp;
        **Dimensions:**
        ...
        Record Valid From
        Record Valid To
    \")")))

(defn generate-uss-section [peripherals]
  (str "## Unified Star Schema\n```mermaid\nflowchart TD\n    %% Entities\n"
       (generate-bridge-entity-block)
       "\n"
       (string/join "\n\n" (map generate-peripheral-entity-block peripherals))
       "\n\n    %% Relations\n"
       "    bridge -- pit_hook__product__id --> source__products\n"
       "    bridge -- pit_hook__customer__id --> source__customers\n"
       "    bridge -- pit_hook__order__id --> source__orders\n"
       "    bridge -- pit_hook__order__product__id --> source__order_lines"
       "\n\n```"))

(defn generate-markdown [config]
  (let [uss (get config :unified-star-schema)
        peripherals (get uss :peripherals)
        frames (get config :frames)
        header "# Documentation\n\n"]
    (str header 
         (generate-hook-section frames) 
         "\n\n" 
         (generate-uss-section peripherals))))

(comment
  (require '[hook-smith.utilities :as utilities])
  
  ;; Load the test data
  (def test-config (utilities/read-yaml-files-in-directory "/workspaces/hook-forge/test/fixtures"))
  
  ;; Generate the markdown and compare with fixture
  (def generated (generate-markdown test-config))
  (def expected (slurp "/workspaces/hook-forge/test/fixtures/documentation.md"))
  (= generated expected)
  
  ;; If they don't match, we can save our output to compare
  (spit "/tmp/generated.md" generated)
  
  ;; We can also run the test to see if it passes
  (require '[hook-smith.documentation-test :as documentation-test])
  (documentation-test/generate-markdown-test)
  )