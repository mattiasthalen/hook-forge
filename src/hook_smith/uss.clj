(ns hook-smith.uss
  (:require [clojure.string :as str]
            [hook-smith.utilities :as utilities]))

(defn get-bridge-path
  "Get the bridge path from the unified star schema."
  [data]
  (->> data
       :unified-star-schema
       :bridge_path))

(defn get-hook-names
  "For each item, combine :hooks and :composite_hooks, returning a vector of hook names, the primary hook name, and foreign hook names."
  [items]
  (mapv (fn [item]
          (let [all-hooks (concat (or (:hooks item) []) (or (:composite_hooks item) []))
                hook-names (mapv :name all-hooks)
                primary-hook (some #(when (:primary %) (:name %)) all-hooks)
                foreign-hooks (vec (remove #(= % primary-hook) hook-names))]
            {:name (:name item)
             :hooks hook-names
             :primary-hook primary-hook
             :foreign-hooks foreign-hooks}))
        items))

(defn left-join-maps
  "Left join map-2 onto map-1 using the given key."
  [map-1 map-2 key]
  (let [map2-by-key (into {} (map (juxt key identity) map-2))]
    (mapv (fn [m1]
            (merge m1 (get map2-by-key (key m1))))
          map-1)))

(defn get-peripheral-data
  [data]
  (let [frames (get data :frames)
        hooks (get-hook-names frames)
        uss (get data :unified-star-schema)
        peripherals (get uss :peripherals)
        joined-data (left-join-maps peripherals hooks :name)]
    joined-data))

(defn generate-dependency-graph
  [data]
  data)

(defn generate-uss-qvs
  "Main function to read YAML configuration files and generate the Qlik script for the Unified Star Schema."
  [path]
  (let [filepath (str path "/generated-uss.qvs")
        data (utilities/read-yaml-files-in-directory path)
        bridge-path (get-bridge-path data)
        peripheral-data (get-peripheral-data data)
        dependency-graph (generate-dependency-graph peripheral-data)]
    dependency-graph))


(comment
  ;; To debug, uncomment the following line:
  ;; (prn (utilities/read-yaml-files-in-directory (str (System/getProperty "user.dir") "/hook")))
  (->>
   (str (System/getProperty "user.dir") "/hook")
   (generate-uss-qvs)))