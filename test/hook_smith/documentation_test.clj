(ns hook-smith.documentation-test
  (:require [clojure.test :refer [deftest is testing]]
            [hook-smith.documentation :as documentation]
            [hook-smith.utilities :as utilities]))

(deftest generate-markdown-test
  (testing "Generates correct markdown documentation"
    (let [config (utilities/read-yaml-files-in-directory "/workspaces/hook-forge/test/fixtures")
          generated-markdown (documentation/generate-markdown config)
          markdown-fixture (slurp "/workspaces/hook-forge/test/fixtures/documentation.md")]
      (is (= markdown-fixture generated-markdown)))))