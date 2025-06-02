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
        foreign-keys (get-foreign-key-hooks frame)
        foreign-keys-section (when (seq foreign-keys)
                               (str "        **Foreign Keys:**\n"
                                    "        " (string/join "\n        " foreign-keys) "\n"
                                    "        &nbsp;\n"))
        skip-generation? (:skip_generation frame)
        [open-bracket close-bracket] (if skip-generation? ["{{" "}}"] ["(" ")"])]
    (str "    frame__" frame-name open-bracket "\"
        **FRAME__" (string/upper-case frame-name) "**
        **Primary Key:**
        " primary-hook "
        &nbsp;
" foreign-keys-section "        **Fields:**
        ...
    \"" close-bracket)))

(defn find-frame-by-primary-hook [frames hook-name]
  (first (filter (fn [frame]
                   (some #(and (= (:name %) hook-name) (:primary %))
                         (concat (:hooks frame) (:composite_hooks frame))))
                 frames)))

(defn generate-frame-relations [frame frames]
  (let [frame-name (:name frame)
        foreign-hooks (get-foreign-key-hooks frame)]
    (filter identity
            (map (fn [hook-name]
                   (let [target-frame (find-frame-by-primary-hook frames hook-name)]
                     (when target-frame
                       (str "    frame__" frame-name " -- " hook-name 
                            " --> frame__" (:name target-frame)))))
                 foreign-hooks))))

(defn generate-hook-relations [frames]
  (let [all-relations (mapcat #(generate-frame-relations % frames) frames)
        grouped-relations (group-by #(re-find #"frame__[^-]+" %) all-relations)]
    
    ;; Join the relations, inserting an empty line between groups
    (->> (map second grouped-relations)
         (filter seq)
         (interpose [""])  ;; Insert empty string between groups
         (apply concat)
         (string/join "\n"))))

(defn generate-hook-section [frames]
  (let [entities (string/join "\n\n" (map generate-hook-entity-block frames))
        relations (generate-hook-relations frames)]
    (str "## Hook\n```mermaid\nflowchart LR\n    %% Entities\n"
         entities
         "\n\n    %% Relations\n"
         relations
         "\n```")))

(defn get-peripheral-primary-key [peripheral frames]
  (let [frame (first (filter #(= (:name %) (:name peripheral)) frames))
        primary-hook (get-primary-hook frame)]
    (str "pit_" primary-hook)))

(defn generate-bridge-entity-block [peripherals frames]
  (let [foreign-keys (map #(get-peripheral-primary-key % frames) peripherals)]
    (str "    bridge(\"
        **_BRIDGE**
        **Primary Key:**
        key__bridge
        &nbsp;
        **Foreign Keys:**
        " (string/join "\n        " foreign-keys) "
        &nbsp;
        **Fields:**
        Peripheral
    \")")))

(defn generate-peripheral-entity-block [peripheral frames]
  (let [name (:name peripheral)
        primary-key (get-peripheral-primary-key peripheral frames)]
    (str "    " name "(\"
        **" (string/upper-case name) "**
        **Primary Key:**
        " primary-key "
        &nbsp;
        **Fields:**
        ...
    \")")))

(defn generate-uss-relations [peripherals frames]
  (map #(str "    bridge -- " (get-peripheral-primary-key % frames) " --> " (:name %)) peripherals))

(defn generate-uss-section [peripherals frames]
  (let [bridge-entity (generate-bridge-entity-block peripherals frames)
        peripheral-entities (string/join "\n\n" (map #(generate-peripheral-entity-block % frames) peripherals))
        relations (string/join "\n" (generate-uss-relations peripherals frames))]
    (str "## Unified Star Schema\n```mermaid\nflowchart TD\n    %% Entities\n"
         bridge-entity
         "\n"
         peripheral-entities
         "\n\n    %% Relations\n"
         relations
         "\n\n```")))

(defn generate-markdown [config]
  (let [uss (get config :unified-star-schema)
        peripherals (get uss :peripherals)
        frames (get config :frames)
        header "# Documentation\n\n"]
    (str header 
         (generate-hook-section frames) 
         "\n\n" 
         (generate-uss-section peripherals frames))))

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