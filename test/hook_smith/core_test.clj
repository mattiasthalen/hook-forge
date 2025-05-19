(ns hook-smith.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [hook-smith.core :as core]
            [clojure.string :as str]))

;; Helper function to capture printed output for testing
(defn with-captured-output [f]
  (let [out (java.io.StringWriter.)]
    (binding [*out* out]
      (f)
      (str out))))

(deftest print-usage-test
  (testing "print-usage outputs expected help text"
    (let [output (with-captured-output core/print-usage)]
      (is (str/includes? output "Usage: hook <command> [options]"))
      (is (str/includes? output "blueprint"))
      (is (str/includes? output "forge"))
      (is (str/includes? output "span"))
      (is (str/includes? output "journal")))))

(deftest blueprint-test
  (testing "blueprint command outputs expected messages"
    (let [output (with-captured-output #(core/blueprint ["--output" "test.yml"]))]
      (is (str/includes? output "Drafting blueprint"))
      (is (str/includes? output "--output test.yml")))))

(deftest forge-test
  (testing "forge command outputs expected messages"
    (let [output (with-captured-output #(core/forge ["frames" "--yaml" "config.yml"]))]
      (is (str/includes? output "Forging frames"))
      (is (str/includes? output "frames --yaml config.yml")))))

(deftest span-test
  (testing "span command outputs expected messages"
    (let [output (with-captured-output #(core/span ["--source" "frames" "--target" "hooks"]))]
      (is (str/includes? output "Building bridge"))
      (is (str/includes? output "--source frames --target hooks")))))

(deftest journal-test
  (testing "journal command outputs expected messages"
    (let [output (with-captured-output #(core/journal ["--output" "docs/"]))]
      (is (str/includes? output "Writing journal"))
      (is (str/includes? output "--output docs/")))))

(deftest command-dispatch-test
  (testing "main function properly dispatches known commands"
    (let [output (with-captured-output #(core/-main "blueprint" "test"))]
      (is (str/includes? output "Drafting blueprint"))))
  
  (testing "main function properly handles unknown commands"
    (let [output (with-captured-output #(core/-main "unknown-command"))]
      (is (str/includes? output "Unknown command: unknown-command"))
      (is (str/includes? output "Usage: hook")))))

(deftest main-test
  (testing "main function with no args prints usage"
    (let [output (with-captured-output #(core/-main))]
      (is (str/includes? output "Usage: hook"))))
  
  (testing "main function with known command invokes the command"
    (let [output (with-captured-output #(core/-main "blueprint" "--output" "test.yml"))]
      (is (str/includes? output "Drafting blueprint"))
      (is (str/includes? output "--output test.yml"))))
  
  (testing "main function with unknown command prints error and usage"
    (let [output (with-captured-output #(core/-main "unknown" "args"))]
      (is (str/includes? output "Unknown command: unknown"))
      (is (str/includes? output "Usage: hook")))))
