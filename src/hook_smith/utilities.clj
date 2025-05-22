(ns hook-smith.utilities
  (:require [clojure.string :as str]
            [clj-yaml.core :as yaml]
            [clojure.java.io :as io]))

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
  "Converts a map to YAML format with blank lines when indentation decreases for better readability."
  [data]
  (yaml/generate-string data :dumper-options {:flow-style :block}))

(defn count-leading-whitespace
  "Return the number of leading whitespace chars (spaces or tabs) in `line`."
  [line]
  (count (re-find #"^[ \t]*" line)))  ;; only spaces or tabs

(defn dedented?
  "True if `curr` is strictly less indented than `prev`."
  [curr prev]
  (< (count-leading-whitespace curr)
     (count-leading-whitespace prev)))

(defn append-with-blank-on-dedent
  "Reducer fn: given accumulated lines `acc` and a new `line`,
  if `line` is dedented relative to the last of `acc`, conj a blank
  line then `line`; otherwise just conj `line`."
  [acc line]
  (if (and (seq acc)
           (dedented? line (peek acc)))
    (conj acc "" line)
    (conj acc line)))

(defn separate-blocks-on-dedent
  "Split string `s` into lines, insert blank lines whenever
  indentation decreases, and re‑join into one string."
  [s]
  (->> (str/split-lines s)
       (reduce append-with-blank-on-dedent [])
       (str/join "\n")))

(defn read-yaml-file
  "Read a YAML file and parse its contents into Clojure data structures"
  [file-path]
  (when (.exists (io/file file-path))
    (yaml/parse-string (slurp file-path))))

(defn read-yaml-files-in-directory
  "Read all yaml files in a directory and return a map with filenames as keys 
   and parsed content as values"
  [dir-path]
  (let [dir (io/file dir-path)
        yaml-files (filter #(str/ends-with? (.getName %) ".yaml")
                           (.listFiles dir))]
    (reduce (fn [acc file]
              (let [filename (str/replace (.getName file) #"\.yaml$" "")]
                (assoc acc (keyword filename) (read-yaml-file (.getPath file)))))
            {}
            yaml-files)))