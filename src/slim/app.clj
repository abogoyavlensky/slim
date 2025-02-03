(ns slim.app
  "A namespace for building uberjars with minimal configuration.
  Provides a simple API for creating standalone JARs from Clojure projects."
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
(def basis
  (delay (b/create-basis {:project "deps.edn"})))

(defn- uber
  "Build an uberjar with the specified parameters.
  
  Parameters:
  :main-ns - main namespace to compile
  :class-dir - directory for compiled classes
  :uber-file - output jar file path
  :src-dirs - source directories to include"
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

(defn- parse-params
  "Parse and validate build parameters, filling in default values.
  
  Parameters:
  :main-ns - main namespace to compile (required)
  :target-dir - target directory (optional)
  :uber-file - output jar file path (optional) 
  :src-dirs - source directories to include (optional)
  :class-dir - directory for compiled classes (optional)
  
  Returns a map with all parameters populated with defaults where not specified."
  [{:keys [target-dir uber-file class-dir src-dirs]
    :as params}]
  (s/assert ::params params)
  (let [target-dir* (or target-dir TARGET-DIR)]
    (assoc params
           :target-dir (or target-dir TARGET-DIR)
           :src-dirs (or src-dirs ["src" "resources"])
           :uber-file (or uber-file (format "%s/standalone.jar" target-dir*))
           :class-dir (or class-dir (format "%s/classes" target-dir*)))))

; Public API

(defn build
  "Build an uberjar.

  :main-ns - main namespace to compile (required)
  :target-dir - target directory (optional, default: target)
  :uber-file - uberjar file (optional, default: target/standalone.jar)
  :src-dirs - source directories (optional, default: [\"src\" \"resources\"])
  :class-dir - class directory (optional, default: target/classes)"
  [params]
  (let [{:keys [target-dir]
         :as params*} (parse-params params)]
    (b/delete {:path target-dir})
    (uber params*)))
