(ns slim.lib-test
  (:require [clojure.test :refer [deftest testing is]]
            [slim.lib :as lib]))

(deftest default-scm-test
  (testing "default-scm with https URL"
    (let [url "https://github.com/user/repo"
          result (#'lib/default-scm url)]
      (is (= {:url url
              :connection "scm:git:git://github.com/user/repo.git"
              :developerConnection "scm:git:ssh://git@github.com/user/repo.git"}
             result))))

  (testing "default-scm with URL containing multiple segments"
    (let [url "https://gitlab.company.com/group/project"
          result (#'lib/default-scm url)]
      (is (= {:url url
              :connection "scm:git:git://gitlab.company.com/group/project.git"
              :developerConnection "scm:git:ssh://git@gitlab.company.com/group/project.git"}
             result)))))

(deftest get-scm-test
  (testing "get-scm with explicit SCM data"
    (let [scm {:url "https://custom.git/repo"
               :connection "custom-connection"
               :developerConnection "custom-dev-connection"}
          result (#'lib/get-scm {:scm scm :version "2.0.0"})]
      (is (= (assoc scm :tag "2.0.0")
             result))))

  (testing "get-scm with URL only"
    (let [url "https://github.com/user/repo"
          result (#'lib/get-scm {:url url :version "1.0.0"})]
      (is (= {:url url
              :tag "1.0.0"
              :connection "scm:git:git://github.com/user/repo.git"
              :developerConnection "scm:git:ssh://git@github.com/user/repo.git"}
             result))))

  (testing "get-scm with no SCM or URL"
    (is (nil? (#'lib/get-scm {:version "1.0.0"})))))

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

(deftest get-license-test
  (testing "get-license with custom license"
    (is (= [:licenses
            [:license
             [:name "Custom License"]
             [:url "https://example.com/license"]]]
           (#'lib/get-license {:name "Custom License"
                               :url "https://example.com/license"}))))

  (testing "get-license with nil license (uses default)"
    (is (= [:licenses
            [:license
             [:name "MIT License"]
             [:url "https://opensource.org/license/mit"]]]
           (#'lib/get-license nil)))))

(deftest pom-template-test
  (testing "pom-template with all fields"
    (is (= [[:description "Test Description"]
            [:url "https://example.com"]
            [:developers
             [:developer
              [:name "Test Developer"]]]
            [:licenses
             [:license
              [:name "Custom License"]
              [:url "https://example.com/license"]]]]
           (#'lib/pom-template {:description "Test Description"
                                :url "https://example.com"
                                :developer "Test Developer"
                                :license {:name "Custom License"
                                          :url "https://example.com/license"}}))))

  (testing "pom-template with only description"
    (is (= [[:description "Test Description"]
            [:licenses
             [:license
              [:name "MIT License"]
              [:url "https://opensource.org/license/mit"]]]]
           (#'lib/pom-template {:description "Test Description"}))))

  (testing "pom-template with only url"
    (is (= [[:url "https://example.com"]
            [:licenses
             [:license
              [:name "MIT License"]
              [:url "https://opensource.org/license/mit"]]]]
           (#'lib/pom-template {:url "https://example.com"}))))

  (testing "pom-template with only developer"
    (is (= [[:developers
             [:developer
              [:name "Test Developer"]]]
            [:licenses
             [:license
              [:name "MIT License"]
              [:url "https://opensource.org/license/mit"]]]]
           (#'lib/pom-template {:developer "Test Developer"}))))

  (testing "pom-template with no fields"
    (is (= [[:licenses
             [:license
              [:name "MIT License"]
              [:url "https://opensource.org/license/mit"]]]]
           (#'lib/pom-template {})))))

(deftest default-license-test
  (testing "default license structure and content"
    (is (= [:licenses
            [:license
             [:name "MIT License"]
             [:url "https://opensource.org/license/mit"]]]
           lib/default-license))))
