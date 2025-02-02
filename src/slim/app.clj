(ns slim.app
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.build.api :as b]))

; Spec

; Enable asserts for spec
(s/check-asserts true)

(s/def ::main-ns symbol?)
(s/def ::target-dir string?)
(s/def ::uber-file string?)
(s/def ::class-dir string?)
(s/def ::src-dirs (s/coll-of string?))

(s/def ::params
  (s/keys
    :req-un [::main-ns]
    :opt-un [::target-dir
             ::uber-file
             ::class-dir
             ::src-dirs]))

; Build

(def ^:private TARGET-DIR "target")

; Delay to defer side effects (artifact downloads)
(def ^:private basis
  (delay (b/create-basis {:project "deps.edn"})))

(defn- uber
  [{:keys [class-dir uber-file main-ns src-dirs]}]
  (b/copy-dir {:src-dirs src-dirs
               :target-dir class-dir})
  (b/compile-clj {:basis @basis
                  :ns-compile [main-ns]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis @basis
           :main main-ns}))

; Public API

(defn build
  "Build an uberjar.

  :main-ns - main namespace to compile (required)
  :target-dir - target directory (optional, default: target)
  :uber-file - uberjar file (optional, default: target/standalone.jar)
  :src-dirs - source directories (optional, default: [\"src\" \"resources\"])
  :class-dir - class directory (optional, default: target/classes)"
  [{:keys [main-ns target-dir uber-file src-dirs class-dir]
    :or {target-dir TARGET-DIR
         src-dirs ["src" "resources"]}
    :as params}]
  (s/assert ::params params)
  (b/delete {:path TARGET-DIR})
  (uber {:uber-file (or uber-file (format "%s/standalone.jar" target-dir))
         :class-dir (or class-dir (format "%s/classes" target-dir))
         :main-ns main-ns
         :src-dirs src-dirs}))
