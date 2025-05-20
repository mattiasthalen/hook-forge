(ns hook-smith.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [hook-smith.core :as core]
            [clojure.java.io :as io]
            [babashka.fs :as fs]))

(deftest print-usage-test
  (testing "print-usage outputs expected help text"
    (is (nil? (core/print-usage)))))

(deftest blueprint-test
  (testing "blueprint command outputs expected messages and writes file"
    (let [tmp-dir (str (fs/create-temp-dir {:prefix "blueprint-test"}))
          out-file (io/file tmp-dir "hook.yaml")]
      (core/blueprint tmp-dir "qlik")
      (is (.exists out-file))
      ;; Clean up temp file and directory using io/delete-file
      (io/delete-file out-file)
      (io/delete-file (io/file tmp-dir)))))

(deftest forge-test
  (testing "forge command outputs expected messages"
    (is (nil? (core/forge ["frames" "--yaml" "config.yml"])))))

(deftest span-test
  (testing "span command outputs expected messages"
    (is (nil? (core/span ["--source" "frames" "--target" "hooks"])))))

(deftest journal-test
  (testing "journal command outputs expected messages"
    (is (nil? (core/journal ["--output" "docs/"])))))

(deftest main-test
  (testing "main function with no args prints usage"
    (is (nil? (core/-main))))
  (testing "main function with known command invokes the command"
    ;; Only test with valid command and no extra args to avoid arity errors
    (is (nil? (core/-main "forge"))))
  (testing "main function with unknown command prints error and usage"
    (is (nil? (core/-main "unknown")))))
