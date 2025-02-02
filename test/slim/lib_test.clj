(ns slim.lib-test
  (:require [clojure.test :refer [deftest testing is]]
            [slim.lib :as lib])
  (:import [clojure.lang ExceptionInfo]))

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
          result (#'lib/get-scm {:scm scm
                                 :version "2.0.0"})]
      (is (= (assoc scm :tag "2.0.0")
             result))))

  (testing "get-scm with URL only"
    (let [url "https://github.com/user/repo"
          result (#'lib/get-scm {:url url
                                 :version "1.0.0"})]
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
           (#'lib/pom-template {:url "https://example.com"})))))

(deftest parse-params-test
  (testing "required params validation"
    (testing "fails without lib"
      (is (thrown? ExceptionInfo
                   (#'lib/parse-params {:version "1.0.0"}))))

    (testing "fails without version"
      (is (thrown? ExceptionInfo
                   (#'lib/parse-params {:lib 'my/lib}))))

    (testing "succeeds with required params"
      (let [result (#'lib/parse-params {:lib 'my/lib
                                        :version "1.0.0"})]
        (is (= 'my/lib (:lib result)))
        (is (= "1.0.0" (:version result))))))

  (testing "important custom params"
    (let [params {:lib 'my/lib
                  :version "1.0.0"
                  :url "https://github.com/user/lib"
                  :developer "John Doe"
                  :description "Test library"}
          result (#'lib/parse-params params)]
      (testing "processes url correctly"
        (is (= "https://github.com/user/lib" (get-in result [:scm :url])))
        (is (string? (get-in result [:scm :connection])))
        (is (string? (get-in result [:scm :developerConnection]))))

      (testing "includes developer in pom-data"
        (is (some #(and (= :developers (first %))
                        (= "John Doe" (get-in % [1 1 1])))
                  (:pom-data result))))

      (testing "includes description in pom-data"
        (is (some #(and (= :description (first %))
                        (= "Test library" (second %)))
                  (:pom-data result))))))

  (testing "optional params"
    (let [params {:lib 'my/lib
                  :version "1.0.0"
                  :target-dir "custom-target"
                  :jar-file "custom.jar"
                  :src-dirs ["src" "extra-src"]
                  :resource-dirs ["resources" "extra-resources"]
                  :class-dir "custom-classes"
                  :snapshot true}
          result (#'lib/parse-params params)]
      (testing "processes target-dir"
        (is (= "custom-target" (:target-dir result))))

      (testing "processes jar-file"
        (is (= "custom.jar" (:jar-file result))))

      (testing "processes src-dirs"
        (is (= ["src" "extra-src"] (:src-dirs result))))

      (testing "processes resource-dirs"
        (is (= ["resources" "extra-resources"] (:resource-dirs result))))

      (testing "processes class-dir"
        (is (= "custom-classes" (:class-dir result))))

      (testing "processes snapshot version"
        (is (= "1.0.0-SNAPSHOT" (:version result))))))

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
