(ns hook-smith.core
  (:require [hook-smith.blueprint :as blueprint]
            [hook-smith.frame :as frame]
            [hook-smith.uss :as uss]
            [hook-smith.utilities :as utilities]
            [hook-smith.documentation :as documentation]))

(defn print-usage []
  (println "Usage: hook <command> [options]")
  (println "")
  (println "Available commands:")
  (println "  blueprint")
  (println "  forge")
  (println "  uss")
  (println "  journal")
  (println ""))

(defn blueprint [path]
  (println "Drafting blueprints...")
  
  (let [blueprint-specs [["concepts" blueprint/concepts-blueprint]
                         ["keysets" blueprint/keysets-blueprint]
                         ["frames" blueprint/frames-blueprint]
                         ["unified-star-schema" blueprint/uss-blueprint]]]
    
    (mapv (partial blueprint/generate-blueprint-file path) blueprint-specs)))

(defn forge [path]
  (println "Forging frames...")
  (frame/generate-hook-qvs path))

(defn uss [path]
  (println "Building Unified Star Schema")
  (let [filepath (str path "/generated-uss.qvs")] 
    (->> path
         (utilities/read-yaml-files-in-directory)
         (uss/generate-uss-qvs)
         (utilities/safe-save filepath))))

(defn journal [path]
  (println "Writing journal...")
  (let [filepath (str path "/generated-documentation.md")]
     (->> path
          (utilities/read-yaml-files-in-directory)
          (documentation/generate-markdown)
          (utilities/safe-save filepath))))

(defn- handle-command 
  "Functionally processes a command and returns the result function to execute"
  [command]
  (or (ns-resolve 'hook-smith.core (symbol command))
      (fn [_]
        (println "Unknown command:" command)
        (print-usage))))

(defn -main
  "Main entry point for the hook-smith CLI."
  [& args]
  (cond
    (empty? args) (print-usage)
    :else (let [command (first args)
                remaining-args (rest args)
                command-fn (handle-command command)
                path (System/getProperty "user.dir")
                full-path (str path "/hook")
                args-with-path (concat [full-path] remaining-args)]
            (apply command-fn args-with-path))))