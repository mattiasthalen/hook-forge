(ns hook-smith.blueprint-test
  (:require [clojure.test :refer [deftest is testing]]
            [hook-smith.blueprint :as blueprint]))

(deftest generate-blueprint-test
  (testing "generate-blueprint returns correct map for known type"
    (let [bp (blueprint/generate-blueprint "qlik")]
      (is (= (:type bp) "qlik"))
      (is (contains? bp :concepts))))
  (testing "generate-blueprint throws for unknown type"
    (is (thrown? Exception (blueprint/generate-blueprint "unknown")))))
