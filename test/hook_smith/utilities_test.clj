(ns hook-smith.utilities-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [hook-smith.utilities :as utilities]
            [clojure.java.io :as io]
            [babashka.fs :as fs])
  (:import [java.io StringWriter]))

;; Create a dynamic var for the temp directory
(def ^:dynamic *test-temp-dir* nil)

;; Helper for capturing stdout
(defn with-out-str-custom [f]
  (let [s (StringWriter.)]
    (binding [*out* s]
      (f)
      (str s))))

;; Fixture for temporary directory
(defn temp-dir-fixture [f]
  (let [tmp-dir (fs/create-temp-dir {:prefix "utilities-test"})]
    (try
      (binding [*test-temp-dir* (str tmp-dir)]
        (f))
      (finally
        (fs/delete-tree tmp-dir)))))

(use-fixtures :each temp-dir-fixture)

(use-fixtures :each temp-dir-fixture)

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

(deftest ensure-parent-dirs-test
  (testing "Creates parent directories when they don't exist"
    (let [file (io/file *test-temp-dir* "subdir" "subsubdir" "test.txt")
          parent-dir (.getParentFile file)
          output (with-out-str-custom #(#'utilities/ensure-parent-dirs file))]
      (is (.exists parent-dir))
      (is (re-find #"Creating directory:" output)))))

(deftest confirm-overwrite-test
  (testing "Returns true when user confirms overwrite with 'y'"
    (with-redefs [read-line (constantly "y")]
      (let [output (with-out-str-custom #(is (#'utilities/confirm-overwrite "test.txt")))]
        (is (re-find #"File already exists: test.txt" output))
        (is (re-find #"Do you want to overwrite it\? \(y/n\):" output)))))
  
  (testing "Returns true when user confirms overwrite with 'Y'"
    (with-redefs [read-line (constantly "Y")]
      (is (#'utilities/confirm-overwrite "test.txt"))))
  
  (testing "Returns false when user denies overwrite"
    (with-redefs [read-line (constantly "n")]
      (is (not (#'utilities/confirm-overwrite "test.txt"))))))

(deftest save-with-notification-test
  (testing "Saves content to file and prints notification"
    (let [file-path (str (io/file *test-temp-dir* "test.txt"))
          content "test content"
          output (with-out-str-custom #(#'utilities/save-with-notification file-path content))]
      (is (re-find #"File saved: .*test.txt" output))
      (is (= content (slurp file-path))))))

(deftest safe-save-test
  (testing "Creates new file with content when file doesn't exist"
    (let [file-path (str (io/file *test-temp-dir* "new_folder" "new_file.txt"))
          content "This is new content"
          output (with-out-str-custom #(utilities/safe-save file-path content))]
      (is (.exists (io/file file-path)))
      (is (= content (slurp file-path)))
      (is (re-find #"Creating directory:" output))
      (is (re-find #"File saved:" output))))
  
  (testing "Overwrites file when user confirms"
    (let [file-path (str (io/file *test-temp-dir* "existing.txt"))
          initial-content "Initial content"
          new-content "New content"]
      ;; Create the file first
      (spit file-path initial-content)
      
      (with-redefs [utilities/confirm-overwrite (constantly true)]
        (let [output (with-out-str-custom #(utilities/safe-save file-path new-content))]
          (is (.exists (io/file file-path)))
          (is (= new-content (slurp file-path)))
          (is (re-find #"File saved:" output))))))
  
  (testing "Cancels operation when user denies overwrite"
    (let [file-path (str (io/file *test-temp-dir* "deny.txt"))
          initial-content "Initial content"
          new-content "This should not be saved"]
      ;; Create the file first
      (spit file-path initial-content)
      
      (with-redefs [utilities/confirm-overwrite (constantly false)]
        (let [output (with-out-str-custom #(is (nil? (utilities/safe-save file-path new-content))))]
          (is (.exists (io/file file-path)))
          (is (= initial-content (slurp file-path)))
          (is (re-find #"Operation cancelled" output)))))))
