(ns slim.lib
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]))

; Spec

; Enable asserts for spec
(s/check-asserts true)

(s/def ::lib symbol?)
(s/def ::version string?)
(s/def ::target-dir string?)
(s/def ::jar-file string?)
(s/def ::class-dir string?)
(s/def ::src-dirs (s/coll-of string?))
(s/def ::resource-dirs (s/coll-of string?))
(s/def ::scm (s/keys
               :req-un [::url]
               :opt-un [::connection
                        ::developerConnection
                        ::tag]))
(s/def ::pom-data (s/coll-of vector? :kind vector?))
(s/def ::license (s/keys
                   :req-un [::name
                            ::url]))
(s/def ::url (s/and string? #(re-matches #"^https://.*" %)))
(s/def ::basis-params map?)
(s/def ::snapshot boolean?)

(s/def ::params
  (s/keys
    :req-un [::lib
             ::version]
    :opt-un [::target-dir
             ::jar-file
             ::class-dir
             ::src-dirs
             ::resource-dirs
             ::scm
             ::pom-data
             ::license
             ::url
             ::basis-params
             ::snapshot]))

; Build

(def ^:private TARGET-DIR "target")
(def ^:private SNAPSHOT-SUFFIX "-SNAPSHOT")

(defn- get-version
  [latest-version snapshot]
  (let [new-version (if (true? snapshot)
                      (str latest-version SNAPSHOT-SUFFIX)
                      latest-version)]
    (println (format "New version: %s" new-version))
    new-version))

(def default-license
  [:licenses
   [:license
    [:name "MIT License"]
    [:url "https://opensource.org/license/mit"]]])

(defn- get-license
  [license]
  (if (seq license)
    [:licenses
     [:license
      [:name (:name license)]
      [:url (:url license)]]]
    default-license))

(defn- pom-template
  [{:keys [url description developer license]}]
  (cond-> []
    (some? description) (conj [:description description])
    (some? url) (conj [:url url])
    (some? developer) (conj [:developers
                             [:developer
                              [:name developer]]])
    true (conj (get-license license))))

(defn- default-scm
  [url]
  (let [url-no-protocol (str/replace-first url #"^https://" "")]
    {:url url
     :connection (format "scm:git:git://%s.git" url-no-protocol)
     :developerConnection (format "scm:git:ssh://git@%s.git" url-no-protocol)}))

(defn- get-scm
  [{:keys [url scm version]}]
  (let [scm* (if (seq scm)
               scm
               (when (some? url)
                 (default-scm url)))]
    ; Return scm data with version if it was provided explicitly or in `url`
    (when (some? scm*)
      ; Add version to scm data only if it was not provided explicitly in `scm` param
      (merge {:tag version} scm*))))

(defn- parse-params
  [{:keys [lib
           version
           target-dir
           jar-file
           src-dirs
           resource-dirs
           class-dir
           scm
           pom-data
           ; custom params
           url
           basis-params
           snapshot]
    :or {snapshot false
         basis-params {:project "deps.edn"}}
    :as params}]
  (s/assert ::params params)
  (let [version* (get-version version snapshot)
        target-dir* (or target-dir TARGET-DIR)]
    (-> params
        (dissoc :version :snapshot :basis-params :url :license)
        (assoc
          :version version*
          :jar-file (or jar-file (format "%s/%s-%s.jar" target-dir* lib version*))
          :basis (b/create-basis basis-params)
          :target-dir target-dir*
          :class-dir (or class-dir (format "%s/classes" target-dir*))
          :src-dirs (or src-dirs ["src"])
          :resource-dirs (or resource-dirs ["resources"])
          :scm (get-scm {:url url
                         :scm scm
                         :version version})
          :pom-data (or pom-data (pom-template params))))))

; Public API

(defn build
  "Build a jar-file for the lib."
  [params]
  (let [{:keys [target-dir src-dirs resource-dirs class-dir]
         :as params*} (parse-params params)]
    (println (format "Building JAR %s..." (:jar-file params*)))
    (b/delete {:path target-dir})
    (b/write-pom params*)
    (b/copy-dir {:src-dirs (concat src-dirs resource-dirs)
                 :target-dir class-dir})
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

(defn create-tag
  "Create a git tag for the lib."
  [{:keys [version]}]
  (b/git-process
    {:git-args ["tag" "-a" version "-m" (format "'Release version %s'" version)]}))

(defn push-tag
  "Push an existing git tag with latest lib version to the remote repository."
  [{:keys [version]}]
  (b/git-process {:git-args ["push" "origin" version]}))
