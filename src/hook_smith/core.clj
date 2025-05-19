(ns hook-smith.core)

(defn print-usage []
  (println "Usage: hook <command> [options]")
  (println "")
  (println "Available commands:")
  (println "  blueprint")
  (println "  forge")
  (println "  span")
  (println "  journal")
  (println ""))

(defn blueprint [args]
  (println "Drafting blueprint...")
  (println "Args:" args))

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
                command-fn (handle-command command)]
            (command-fn remaining-args))))