(ns hook-smith.core
  (:require [hook-smith.blueprint :as blueprint]
            [hook-smith.frame :as frame]
            [hook-smith.uss :as uss]))

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
  (uss/generate-uss-bridge path))

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