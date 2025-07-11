(ns hook-smith.frame-test
  (:require [clojure.test :refer [deftest is testing]]
            [hook-smith.frame :as frame]))

(deftest generate-frame-header-test
  (testing "Generates correct frame header string"
    (let [frame {:name "TestTable"
                 :source_table "/source/path"
                 :target_table "/target/path"}
          header (frame/generate-frame-header frame)]
      (is (re-find #"Table: TestTable" header))
      (is (re-find #"Source: /source/path" header))
      (is (re-find #"Target: /target/path" header)))))

(deftest generate-hook-field-test
  (testing "Generates correct hook field string with default expression"
    (let [hook {:name "hook1" :keyset "ks1" :business_key_field "bkf1"}
          field (frame/generate-hook-field hook)]
      (is (= field "\tIf(Not IsNull(Text([bkf1])), 'ks1|' & Text([bkf1]))\tAs [hook1]"))))
  
  (testing "Generates correct hook field string with custom expression"
    (let [hook {:name "hook2" :keyset "ks2" :business_key_field "bkf2" :expression "SubField([order_number], '-', 1)"}
          field (frame/generate-hook-field hook)]
      (is (= field "\tIf(Not IsNull(SubField([order_number], '-', 1)), 'ks2|' & SubField([order_number], '-', 1))\tAs [hook2]")))))

(deftest generate-frame-hooks-test
  (testing "Generates joined hook fields"
    (let [frame {:hooks [{:name "h1" :keyset "ks1" :business_key_field "bk1"}
                         {:name "h2" :keyset "ks2" :business_key_field "bk2"}]}
          hooks-str (frame/generate-frame-hooks frame)]
      (is (re-find #"\[h1\]" hooks-str))
      (is (re-find #"\[h2\]" hooks-str))
      (is (re-find #"," hooks-str)))))

(deftest generate-composite-hooks-test
  (testing "Generates composite hook definitions"
    (let [frame {:composite_hooks [{:name "comp1" :hooks ["h1" "h2"]}
                                   {:name "comp2" :hooks ["h3" "h4"]}]}
          comp-str (frame/generate-composite-hooks frame)]
      (is (re-find #"\[h1\]" comp-str))
      (is (re-find #"\[h2\]" comp-str))
      (is (re-find #"As \[comp1\]" comp-str))
      (is (re-find #"As \[comp2\]" comp-str)))))

(deftest generate-frame-script-test
  (testing "Generates full QVS script for a frame"
    (let [frame {:name "TestTable"
                 :source_table "src.qvd"
                 :target_table "tgt.qvd"
                 :hooks [{:name "h1" :keyset "ks1" :business_key_field "bk1"}]
                 :composite_hooks [{:name "comp1" :hooks ["h1"]}]}
          script (frame/generate-frame-script frame)]
      (is (re-find #"TestTable" script))
      (is (re-find #"src.qvd" script))
      (is (re-find #"tgt.qvd" script))
      (is (re-find #"\[h1\]" script))
      (is (re-find #"As \[comp1\]" script)))))

(deftest generate-qvs-script-test
  (testing "Generates QVS script for multiple frames"
    (let [frames [{:name "A" :source_table "a.qvd" :target_table "a2.qvd" :hooks []}
                  {:name "B" :source_table "b.qvd" :target_table "b2.qvd" :hooks []}]
          script (frame/generate-qvs-script frames)]
      (is (re-find #"A" script))
      (is (re-find #"B" script))
      (is (re-find #"a.qvd" script))
      (is (re-find #"b2.qvd" script)))))

(deftest generate-qvs-script-skip-generation-test
  (testing "Skips frames with skip_generation set to true"
    (let [frames [{:name "A" :source_table "a.qvd" :target_table "a2.qvd" :hooks []}
                  {:name "B" :source_table "b.qvd" :target_table "b2.qvd" :hooks [] :skip_generation true}
                  {:name "C" :source_table "c.qvd" :target_table "c2.qvd" :hooks []}]
          script (frame/generate-qvs-script frames)]
      (is (re-find #"A" script))
      (is (not (re-find #"B" script)))
      (is (re-find #"C" script))
      (is (re-find #"a.qvd" script))
      (is (not (re-find #"b.qvd" script)))
      (is (re-find #"c2.qvd" script)))))
