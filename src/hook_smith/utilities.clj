(ns hook-smith.utilities
  (:require [clojure.string :as str]
            [clj-yaml.core :as yaml]))

(defn safe-save
  "Safely save content to a file, prompting for confirmation if the file exists."
  [file-path content]
  (let [file (java.io.File. file-path)]
    (if (.exists file)
      (do
        (println (str "File already exists: " file-path))
        (print "Do you want to overwrite it? (y/n): ")
        (flush)
        (let [answer (read-line)]
          (if (= (clojure.string/lower-case answer) "y")
            (do
              (spit file-path content)
              (println (str "File saved: " file-path)))
            (println "Operation cancelled."))))
      (do
        (spit file-path content)
        (println (str "File saved: " file-path))))))

(defn convert-map-to-yaml
  "Converts a map to YAML format."
  [data]
  (yaml/generate-string data :dumper-options {:flow-style :block}))