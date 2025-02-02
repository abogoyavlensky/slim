(ns slim.lib
  (:require [clojure.tools.build.api :as b]
            [clojure.spec.alpha :as s]))

; Spec

; Enable asserts for spec
(s/check-asserts true)


; Build

(def ^:private TARGET-DIR "target")
(def ^:private SNAPSHOT-SUFFIX "-SNAPSHOT")

(defn- get-version
  [latest-version snapshot?]
  (let [new-version (if (true? snapshot?)
                      (str latest-version SNAPSHOT-SUFFIX))]
    (println (format "New version: %s" new-version))
    new-version))

(defn create-tag
  "Create a git tag for the lib."
  [{version-name :version
    :as opts}]
  (b/git-process
    {:git-args ["tag" "-a" version-name "-m" (format "'Release version %s'" version-name)]})
  opts)

(defn push-tag
  "Push an existing git tag with latest lib version to the remote repository."
  [{version-name :version
    :as opts}]
  (b/git-process {:git-args ["push" "origin" version-name]})
  opts)

(defn build
  "Build a jar-file for the lib."
  [{:keys [lib
           version
           tag
           snapshot?
           target-dir
           jar-file
           src-dirs
           resource-dirs
           class-dir
           scm
           basis-params]
    :or {target-dir TARGET-DIR
         src-dirs ["src"]
         resource-dirs ["resources"]
         snapshot? false
         basis-params {:project "deps.edn"}}
    :as params}]
  ; TODO: add params validation!

  (let [version* (get-version version snapshot?)
        class-dir* (or class-dir (format "%s/classes" target-dir))
        scm* (merge {:tag (or tag version)} scm)
        params* (-> params
                    (dissoc :version :snapshot? :basis-params)
                    (assoc
                      :version version*
                      :tag (or tag version)
                      :jar-file (or jar-file (format "%s/%s-%s.jar" target-dir lib version*))
                      :basis (b/create-basis basis-params)
                      :class-dir class-dir*
                      :scm scm*))]

    (println (format "Building JAR %s..." (:jar-file params*)))
    (b/delete {:path target-dir})
    (b/write-pom params*)
    (b/copy-dir {:src-dirs (concat src-dirs resource-dirs)
                 :target-dir class-dir*})
    (b/jar params*)
    (println "JAR has been built successfully!")))


;(defn install
;  "Build and install jar-file to the local repo."
;  [opts]
;  (-> opts
;      (build)
;      (build-clj/install)))
;
;
;(defn deploy
;  "Build and deploy the jar-file to Clojars."
;  [opts]
;  (-> opts
;      (build)
;      (build-clj/deploy)))
