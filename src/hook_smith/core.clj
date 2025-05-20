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
         (utilities/convert-map-to-yaml)
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