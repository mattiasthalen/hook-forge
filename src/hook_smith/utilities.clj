(ns hook-smith.utilities
  (:require [clojure.string :as str]
            [clj-yaml.core :as yaml]))

(defn- ensure-parent-dirs
  "Create parent directories if they don't exist."
  [file]
  (let [parent-dir (.getParentFile file)]
    (when (and parent-dir (not (.exists parent-dir)))
      (println (str "Creating directory: " (.getPath parent-dir)))
      (.mkdirs parent-dir))))

(defn- confirm-overwrite
  "Prompt for confirmation if file exists. Returns true if user confirms."
  [file-path]
  (println (str "File already exists: " file-path))
  (print "Do you want to overwrite it? (y/n): ")
  (flush)
  (let [answer (read-line)]
    (= (str/lower-case answer) "y")))

(defn- save-with-notification
  "Save content to file and print notification."
  [file-path content]
  (spit file-path content)
  (println (str "File saved: " file-path))
  file-path)

(defn safe-save
  "Safely save content to a file, prompting for confirmation if the file exists.
   Creates parent directories if they don't exist."
  [file-path content]
  (let [file (java.io.File. file-path)]
    (ensure-parent-dirs file)
    (if (.exists file)
      (if (confirm-overwrite file-path)
        (save-with-notification file-path content)
        (do 
          (println "Operation cancelled.")
          nil))
      (save-with-notification file-path content))))

(defn convert-map-to-yaml
  "Converts a map to YAML format."
  [data]
  (yaml/generate-string data :dumper-options {:flow-style :block}))