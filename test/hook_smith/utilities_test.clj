(ns hook-smith.utilities-test
  (:require [clojure.test :refer [deftest is testing]]
            [hook-smith.utilities :as utilities]))

(deftest convert-map-to-yaml-test
  (testing "Converts a map to YAML format"
    (let [yaml-string (utilities/convert-map-to-yaml {:project_name "Test Project"
                                                     :type "qlik"
                                                     :concepts [{:name "concept1" :type "core"}]})]
      (is (re-find #"project_name: Test Project" yaml-string))
      (is (re-find #"type: qlik" yaml-string))
      (is (re-find #"concepts:" yaml-string))
      (is (re-find #"- name: concept1" yaml-string))
      (is (re-find #"type: core" yaml-string)))))
