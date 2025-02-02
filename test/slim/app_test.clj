(ns slim.app-test
  (:require [bond.james :as bond]
            [clojure.test :refer :all]
            [clojure.tools.build.api :as b]
            [slim.app :as app])
  (:import [clojure.lang ExceptionInfo]))

(deftest test-app-build-params-validation-with-required-params-only
  #_{:clj-kondo/ignore [:private-call]}
  (bond/with-stub! [[b/delete (constantly nil)]
                    [app/uber (constantly nil)]]

    (app/build {:main-ns 'my-app.core})

    (is (= 1 (-> b/delete bond/calls count)))
    (is (= [{:path "target"}]
           (-> b/delete bond/calls first :args)))
    (is (= 1 (-> #'app/uber bond/calls count)))
    (is (= [{:class-dir "target/classes"
             :main-ns 'my-app.core
             :target-dir "target"
             :src-dirs ["src"
                        "resources"]
             :uber-file "target/standalone.jar"}]
           (-> #'app/uber bond/calls first :args)))))

(deftest test-app-build-params-validation-all-optional-params
  #_{:clj-kondo/ignore [:private-call]}
  (bond/with-stub! [[b/delete (constantly nil)]
                    [app/uber (constantly nil)]]

    (app/build {:main-ns 'my-app.core
                :target-dir "custom-target"
                :uber-file "custom-target/app.jar"
                :src-dirs ["custom-src"]
                :class-dir "custom-target/custom-classes"})

    (is (= 1 (-> b/delete bond/calls count)))
    (is (= [{:path "custom-target"}]
           (-> b/delete bond/calls first :args)))
    (is (= 1 (-> #'app/uber bond/calls count)))
    (is (= [{:class-dir "custom-target/custom-classes"
             :target-dir "custom-target"
             :main-ns 'my-app.core
             :src-dirs ["custom-src"]
             :uber-file "custom-target/app.jar"}]
           (-> #'app/uber bond/calls first :args)))))

(deftest test-app-build-params-validation-with-invalid-params
  (testing "build with invalid params should throw assertion error"
    (is (thrown-with-msg? ExceptionInfo #"^Spec assertion failed"
                          (app/build {})))

    (is (thrown-with-msg? ExceptionInfo #"^Spec assertion failed"
                          (app/build {:main-ns "not-a-symbol"})))))
