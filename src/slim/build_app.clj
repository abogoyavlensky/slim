(ns slim.build-app
    (:require [clojure.tools.build.api :as b]))

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

(defn build
  "Build an uberjar.

  :main-ns - main namespace to compile (required)
  :target-dir - target directory (optional, default: target)
  :uber-file - uberjar file (optional, default: target/standalone.jar)
  :src-dirs - source directories (optional, default: [\"src\" \"resources\"])
  :class-dir - class directory (optional, default: target/classes)"
  [{:keys [main-ns target-dir uber-file src-dirs class-dir]
    :or {target-dir TARGET-DIR
         src-dirs ["src" "resources"]}}]
  {:pre [(symbol? main-ns)]}
  (b/delete {:path TARGET-DIR})
  (uber {:uber-file (or uber-file (format "%s/standalone.jar" target-dir))
         :class-dir (or class-dir (format "%s/classes" target-dir))
         :main-ns main-ns
         :src-dirs src-dirs}))
