(ns hook-smith.uss-test
  (:require [clojure.test :refer [deftest is testing]]
            [hook-smith.uss :as uss]
            [hook-smith.utilities :as utilities]))

#_(deftest generate-uss-qvs-test
  (testing "Generates correct QVS script for USS"
    (let [config (utilities/read-yaml-files-in-directory "/workspaces/hook-forge/test/fixtures")
          generated-script (uss/generate-uss-qvs config)
          script-fixture (slurp "/workspaces/hook-forge/test/fixtures/unified-star-schema.qvs")]
      (is (= script-fixture generated-script)))))