(ns hook-smith.core-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [hook-smith.core :as core]
            [hook-smith.blueprint :as blueprint]
            [babashka.fs :as fs])
  (:import [java.io StringWriter]))

;; Helper for capturing stdout
(defn with-out-str-custom [f]
  (let [s (StringWriter.)]
    (binding [*out* s]
      (f)
      (str s))))

;; Create a dynamic var for the temp directory
(def ^:dynamic *test-temp-dir* nil)

;; Fixture for temporary directory
(defn temp-dir-fixture [f]
  (let [tmp-dir (fs/create-temp-dir {:prefix "core-test"})]
    (try
      (binding [*test-temp-dir* (str tmp-dir)]
        (f))
      (finally
        (fs/delete-tree tmp-dir)))))

(use-fixtures :each temp-dir-fixture)

(deftest print-usage-test
  (testing "print-usage outputs expected help text"
    (let [output (with-out-str-custom #(core/print-usage))]
      (is (re-find #"Usage: hook <command>" output))
      (is (re-find #"blueprint" output))
      (is (re-find #"forge" output))
      (is (re-find #"uss" output))
      (is (re-find #"journal" output)))))

(deftest blueprint-test
  (testing "blueprint command creates expected files"
    (with-redefs [blueprint/generate-blueprint-file (fn [path [name _]] 
                                                      (str path "/" name ".yaml"))]
      (let [output (with-out-str-custom #(core/blueprint *test-temp-dir*))
            result (core/blueprint *test-temp-dir*)]
        (is (re-find #"Drafting blueprints" output))
        (is (vector? result))
        (is (= 3 (count result)))
        (is (some #(re-find #"/concepts\.yaml$" %) result))
        (is (some #(re-find #"/keysets\.yaml$" %) result))
        (is (some #(re-find #"/frames\.yaml$" %) result))))))

(deftest forge-test
  (testing "forge command outputs expected messages"
    (let [output (with-out-str-custom #(core/forge *test-temp-dir*))]
      (is (re-find #"Forging frames" output)))))

(deftest uss-test
  (testing "uss command outputs expected messages"
    (let [output (with-out-str-custom #(core/uss ["--source" "frames" "--target" "hooks"]))]
      (is (re-find #"Building Unified Star Schema" output))
      (is (re-find #"--source frames --target hooks" output)))))

(deftest journal-test
  (testing "journal command outputs expected messages"
    (let [output (with-out-str-custom #(core/journal ["--output" "docs/"]))]
      (is (re-find #"Writing journal" output))
      (is (re-find #"--output docs/" output)))))

(deftest handle-command-test
  (testing "handle-command returns function for known command"
    (let [command-fn (#'core/handle-command "blueprint")]
      (is (var? command-fn))
      (is (= (var-get command-fn) core/blueprint))))
  
  (testing "handle-command returns error function for unknown command"
    (let [command-fn (#'core/handle-command "unknown")
          output (with-out-str-custom #(command-fn []))]
      (is (fn? command-fn))
      (is (re-find #"Unknown command: unknown" output))
      (is (re-find #"Usage:" output)))))

(deftest main-test
  (testing "main function with no args prints usage"
    (let [output (with-out-str-custom #(core/-main))]
      (is (re-find #"Usage:" output))))
  
  (testing "main function with known command invokes the command"
    (with-redefs [core/blueprint (fn [& args] 
                                   (is (= (first args) 
                                          (str (System/getProperty "user.dir") "/hook"))))]
      (core/-main "blueprint")))
  
  (testing "main function with unknown command prints error and usage"
    (let [output (with-out-str-custom #(core/-main "unknown"))]
      (is (re-find #"Unknown command: unknown" output))
      (is (re-find #"Usage:" output)))))
