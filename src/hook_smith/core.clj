(ns hook-smith.core
  (:require [hook-smith.blueprint :as blueprint]
            [hook-smith.utilities :as utilities]))

(defn print-usage []
  (println "Usage: hook <command> [options]")
  (println "")
  (println "Available commands:")
  (println "  blueprint")
  (println "  forge")
  (println "  span")
  (println "  journal")
  (println ""))

(defn blueprint [path type]
  (let [file-path (str path "/hook.yaml")
        type-str (if (vector? type) (first type) type)]
    (println "Drafting blueprint...")
    (->> (blueprint/generate-blueprint type-str)
         (blueprint/convert-map-to-yaml)
         (utilities/safe-save file-path))))

(defn forge [args]
  (println "Forging frames...")
  (println "Args:" args))

(defn span [args]
  (println "Building bridge...")
  (println "Args:" args))

(defn journal [args]
  (println "Writing journal...")
  (println "Args:" args))

(defn- handle-command 
  "Functionally processes a command and returns the result function to execute"
  [command]
  (or (ns-resolve 'hook-smith.core (symbol command))
      (fn [_]
        (println "Unknown command:" command)
        (print-usage))))

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

(defn -main
  "Main entry point for the hook-smith CLI."
  [& args]
  (cond
    (empty? args) (print-usage)
    :else (let [command (first args)
                remaining-args (rest args)
                command-fn (handle-command command)
                path (System/getProperty "user.dir")
                args-with-path (concat [path] remaining-args)]
            (apply command-fn args-with-path))))