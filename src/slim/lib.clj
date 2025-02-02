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
  [{:keys [version]}]
  (b/git-process
    {:git-args ["tag" "-a" version "-m" (format "'Release version %s'" version)]}))

(defn push-tag
  "Push an existing git tag with latest lib version to the remote repository."
  [{:keys [version]}]
  (b/git-process {:git-args ["push" "origin" version]}))

(defn- get-license
  [license]
  (if (seq license)
    [:licenses
     [:license
      [:name (:name license)]
      [:url (:url license)]]]
    [:licenses
     [:license
      [:name "MIT License"]
      [:url "https://opensource.org/license/mit/"]]]))

(defn- pom-template
  [{:keys [url description developer license]}]
  (cond-> []
    (some? description) (conj [:description description])
    (some? url) (conj [:url url])
    (some? developer) (conj [:developers
                             [:developer
                              [:name developer]]])
    true (conj (get-license license))))

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
           pom-data
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
                      :jar-file (or jar-file (format "%s/%s-%s.jar" target-dir lib version*))
                      :basis (b/create-basis basis-params)
                      :class-dir class-dir*
                      :scm scm*
                      :pom-data (or pom-data (pom-template params))))]

    (println (format "Building JAR %s..." (:jar-file params*)))
    (b/delete {:path target-dir})
    (b/write-pom params*)
    (b/copy-dir {:src-dirs (concat src-dirs resource-dirs)
                 :target-dir class-dir*})
    (b/jar params*)
    (println "JAR has been built successfully!")
    params*))

(defn install
  "Build and install jar-file to the local repo."
  [params]
  (-> params
      (build)
      (b/install))
  (println "JAR has been installed to local repo successfully!"))

(defn deploy
  "Build and deploy the jar-file to Clojars."
  [params]
  (let [{:keys [jar-file lib class-dir]} (build params)
        ; Require deploy function dynamically to avoid dependency
        ; if Slim is used just for building application uberjar.
        ; To build and deploy the lib, add the following deps:
        ; slipset/deps-deploy {:mvn/version "LATEST"}
        deploy-fn (requiring-resolve 'deps-deploy.deps-deploy/deploy)]
    (println "Deploying JAR to Clojars...")
    (deploy-fn {:installer :remote
                :artifact (b/resolve-path jar-file)
                :pom-file (b/pom-path {:lib lib
                                       :class-dir class-dir})})
    (println "JAR has been deployed successfully!")))
