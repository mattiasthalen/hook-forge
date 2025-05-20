(ns hook-smith.blueprint)

(def concepts-blueprint
  "Generates a concepts block for the blueprint."
  [{:name "concept"
    :type "core"}])

(def keysets-blueprint
  "Generates a keysets block for the blueprint."
   [{:name "source.tenant.concept"
     :concept "concept"
     :source_system "source"
     :tenant "tenant"}])

(def qlik-frames-blueprint
  "Generates a frames block for the blueprint."
  [{:name "frame__source__concept"
    :source_system "source"
    :source_path "lib://path/to/concept.qvd"
    :target_path "lib://path/to/frame.qvd"
    :hooks [{:name "hook__concept__qualifier"
             :primary false
             :concept "concept"
             :qualifier "qualifier"
             :keyset "source.tenant.concept"
             :business_key_field "business_key"}]
    :composite_hooks [{:name "hook__concept_concept__qualifier"
                       :primary true
                       :hooks ["hook__concept__qualifier" "hook__concept__qualifier"]}]}])

(def generate-qlik-blueprint
  "Generates a blueprint for Qlik."
  {:project_name "Project Name"
   :type "qlik"

   :concepts concepts-blueprint
   :keysets keysets-blueprint
   :frames qlik-frames-blueprint})

(defn generate-blueprint
  "Generates a blueprint for the given type."
  [type]
  (case type
    "qlik" generate-qlik-blueprint
    (throw (ex-info "Unknown type" {:type type}))))