(ns slim.lib-test
  (:require [bond.james :as bond]
            [clojure.test :refer :all]
            [clojure.tools.build.api :as b]
            [slim.lib :as lib])
  (:import [clojure.lang ExceptionInfo]
           [java.io File]))

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

  (testing "get-scm with snapshot version"
    (with-redefs [lib/git-sha-latest (constantly "abc123")]
      (let [url "https://github.com/user/repo"
            result (#'lib/get-scm {:scm-url url
                                   :url "https://test.com"
                                   :version "1.0.0"
                                   :snapshot true})]
        (is (= {:url "https://github.com/user/repo"
                :tag "abc123"
                :connection "scm:git:git://github.com/user/repo.git"
                :developerConnection "scm:git:ssh://git@github.com/user/repo.git"}
               result)))))

  (testing "get-scm with custom SCM and snapshot version"
    (with-redefs [lib/git-sha-latest (constantly "def456")]
      (let [scm {:url "https://custom.git/repo"
                 :connection "custom-connection"
                 :developerConnection "custom-dev-connection"}
            result (#'lib/get-scm {:scm scm
                                   :version "2.0.0"
                                   :snapshot true})]
        (is (= (assoc scm :tag "def456")
               result)))))

  (testing "get-scm with no SCM or URL"
    (is (= {:tag "1.0.0"} (#'lib/get-scm {:version "1.0.0"})))))

(deftest process-version-template-test
  (testing "process-version-template with no template variables"
    (is (= "1.0.0"
           (#'lib/process-version-template "1.0.0"))))

  (testing "process-version-template with git-count-revs template variable"
    (with-redefs [b/git-count-revs (constantly 42)]
      (is (= "1.0.42"
             (#'lib/process-version-template "1.0.{{git-count-revs}}")))))

  (testing "process-version-template with git-count-revs in the middle"
    (with-redefs [b/git-count-revs (constantly 123)]
      (is (= "1.123.0"
             (#'lib/process-version-template "1.{{git-count-revs}}.0"))))))

(deftest get-version-test
  (testing "get-version without snapshot"
    (is (= "1.0.0"
           (#'lib/get-version {:version "1.0.0"
                               :snapshot false}))))

  (testing "get-version with snapshot"
    (is (= "1.0.0-SNAPSHOT"
           (#'lib/get-version {:version "1.0.0"
                               :snapshot true}))))

  (testing "get-version with nil snapshot (defaults to false)"
    (is (= "2.1.0"
           (#'lib/get-version {:version "2.1.0"
                               :snapshot nil}))))

  (testing "get-version with template variable and snapshot"
    (with-redefs [b/git-count-revs (constantly 99)]
      (is (= "1.99.0-SNAPSHOT"
             (#'lib/get-version {:version "1.{{git-count-revs}}.0"
                                 :snapshot true})))))

  (testing "get-version with template variable without snapshot"
    (with-redefs [b/git-count-revs (constantly 88)]
      (is (= "0.88.1"
             (#'lib/get-version {:version "0.{{git-count-revs}}.1"
                                 :snapshot false}))))))

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

(deftest test-build-params-validation-with-required-params-only-and-writing-pom
  (bond/with-spy [b/write-pom]
    (bond/with-stub! [[b/delete (constantly nil)]
                      [b/copy-dir (constantly nil)]
                      [b/jar (constantly nil)]]

      (lib/build {:lib 'io.test.my-app/another-lib
                  :version "1.0.0"
                  :target-dir "test/target"})

      (is (= 1 (-> b/delete bond/calls count)))
      (is (= [{:path "test/target"}]
             (-> b/delete bond/calls first :args)))

      (is (= 1 (-> b/write-pom bond/calls count)))
      (is (= {:class-dir "test/target/classes"
              :jar-file "test/target/io.test.my-app/another-lib-1.0.0.jar"
              :lib 'io.test.my-app/another-lib
              :pom-data [[:licenses
                          [:license
                           [:name
                            "MIT License"]
                           [:url
                            "https://opensource.org/license/mit"]]]]
              :resource-dirs ["resources"]
              :scm {:tag "1.0.0"}
              :src-dirs ["src"]
              :target-dir "test/target"
              :version "1.0.0"}
             (-> b/write-pom bond/calls first :args first (dissoc :basis))))

      (is (= 1 (-> b/copy-dir bond/calls count)))
      (is (= [{:src-dirs ["src" "resources"]
               :target-dir "test/target/classes"}]
             (-> b/copy-dir bond/calls first :args))))))

(deftest test-build-custom-params-only-with-writing-pom
  (bond/with-spy [b/write-pom]
    (bond/with-stub! [[b/delete (constantly nil)]
                      [b/copy-dir (constantly nil)]
                      [b/jar (constantly nil)]]

      (lib/build {:lib 'io.test.my-app/another-lib
                  :version "1.0.0"
                  :url "https://github.io/test/my-app"
                  :developer "John Doe"
                  :description "Test library"
                  :target-dir "test/target"})

      (is (= 1 (-> b/delete bond/calls count)))
      (is (= [{:path "test/target"}]
             (-> b/delete bond/calls first :args)))

      (is (= 1 (-> b/write-pom bond/calls count)))
      (is (= {:class-dir "test/target/classes"
              :description "Test library"
              :developer "John Doe"
              :jar-file "test/target/io.test.my-app/another-lib-1.0.0.jar"
              :lib 'io.test.my-app/another-lib
              :pom-data [[:description
                          "Test library"]
                         [:url
                          "https://github.io/test/my-app"]
                         [:developers
                          [:developer
                           [:name
                            "John Doe"]]]
                         [:licenses
                          [:license
                           [:name
                            "MIT License"]
                           [:url
                            "https://opensource.org/license/mit"]]]]
              :resource-dirs ["resources"]
              :scm {:connection "scm:git:git://github.io/test/my-app.git"
                    :developerConnection "scm:git:ssh://git@github.io/test/my-app.git"
                    :tag "1.0.0"
                    :url "https://github.io/test/my-app"}
              :src-dirs ["src"]
              :target-dir "test/target"
              :version "1.0.0"}
             (-> b/write-pom bond/calls first :args first (dissoc :basis))))

      (is (= 1 (-> b/copy-dir bond/calls count)))
      (is (= [{:src-dirs ["src" "resources"]
               :target-dir "test/target/classes"}]
             (-> b/copy-dir bond/calls first :args))))))

(deftest read-version-from-file-test
  (testing "read-version-from-file with valid file"
    (let [temp-file (File/createTempFile "version" ".txt")]
      (try
        (spit temp-file "1.2.3")
        (is (= "1.2.3" (#'lib/read-version-from-file (.getPath temp-file))))
        (finally
          (.delete temp-file)))))

  (testing "read-version-from-file with file containing whitespace"
    (let [temp-file (File/createTempFile "version" ".txt")]
      (try
        (spit temp-file "  2.0.0  \n")
        (is (= "2.0.0" (#'lib/read-version-from-file (.getPath temp-file))))
        (finally
          (.delete temp-file)))))

  (testing "read-version-from-file with non-existent file"
    (is (thrown-with-msg? ExceptionInfo #"Failed to read version from file"
                          (#'lib/read-version-from-file "non-existent-file.txt")))))

(deftest parse-params-with-version-file-test
  (testing "parse-params with version-file"
    (let [temp-file (File/createTempFile "version" ".txt")]
      (try
        (spit temp-file "3.0.0")
        (let [result (#'lib/parse-params {:lib 'my/lib
                                          :version-file (.getPath temp-file)})]
          (is (= "3.0.0" (:version result))))
        (finally
          (.delete temp-file)))))

  (testing "parse-params with both version and version-file (version-file takes precedence)"
    (let [temp-file (File/createTempFile "version" ".txt")]
      (try
        (spit temp-file "4.0.0")
        (let [result (#'lib/parse-params {:lib 'my/lib
                                          :version "2.0.0"
                                          :version-file (.getPath temp-file)})]
          (is (= "4.0.0" (:version result))))
        (finally
          (.delete temp-file)))))

  (testing "parse-params with neither version nor version-file"
    (is (thrown-with-msg? ExceptionInfo #"Spec assertion failed"
                          (#'lib/parse-params {:lib 'my/lib})))))
