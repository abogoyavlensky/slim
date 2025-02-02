(ns slim.lib-test
  (:require [clojure.test :refer [deftest testing is]]
            [slim.lib :as lib]))

(deftest get-version-test
  (testing "get-version without snapshot"
    (is (= "1.0.0"
           (#'lib/get-version "1.0.0" false))))

  (testing "get-version with snapshot"
    (is (= "1.0.0-SNAPSHOT"
           (#'lib/get-version "1.0.0" true))))

  (testing "get-version with nil snapshot (defaults to false)"
    (is (= "2.1.0"
           (#'lib/get-version "2.1.0" nil)))))
