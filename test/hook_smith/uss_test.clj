(ns hook-smith.uss-test
  (:require [clojure.test :refer [deftest is testing]]
            [hook-smith.uss :as uss]
            [hook-smith.utilities :as utilities]))

(deftest generate-uss-qvs-test
  (testing "Generates correct QVS script for USS"
    (let [config (utilities/read-yaml-files-in-directory "/workspaces/hook-forge/test/fixtures")
          generated-script (uss/generate-uss-qvs config)
          ;; Read fixture with BOM - slurp will read the BOM character as \uFEFF
          raw-fixture (slurp "/workspaces/hook-forge/test/fixtures/unified-star-schema.qvs" :encoding "UTF-8")
          ;; Remove BOM character for comparison since generate-uss-qvs returns content without BOM
          script-fixture (if (and (>= (count raw-fixture) 1)
                                  (= (first raw-fixture) \uFEFF))
                           (subs raw-fixture 1) ; Remove BOM character
                           raw-fixture)]
      (is (= script-fixture generated-script)))))