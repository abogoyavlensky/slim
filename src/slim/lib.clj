(ns slim.lib
  "A set of functions for building and deploying Clojure libraries.

  The main functions are:
  - build: Create a JAR file
  - install: Build and install to local repo
  - deploy: Build and deploy to Clojars
  - tag: Create a Git tag for the version"
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]))

; Spec

; Enable asserts for spec
(s/check-asserts true)

(s/def ::lib symbol?)
(s/def ::version string?)
(s/def ::version-file string?)
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
    :req-un [(or ::version ::version-file)
             ::lib]
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
             ::snapshot
             ::version
             ::version-file]))

; Build

(def ^:private TARGET-DIR "target")
(def ^:private SNAPSHOT-SUFFIX "-SNAPSHOT")

(defn- process-version-template
  "Processes version string template by replacing variables with actual values.
  
  Parameters:
  - version (string): The version string that may contain template variables
  
  Returns:
  - string: The processed version string with template variables replaced"
  [version]
  (let [commit-count-pattern #"\{\{git-count-revs\}\}"
        has-commit-count-var (re-find commit-count-pattern version)]
    (if has-commit-count-var
      (let [commit-count (b/git-count-revs nil)]
        (str/replace version commit-count-pattern (str commit-count)))
      version)))

(defn- read-version-from-file
  "Reads version string from a file.

  Parameters:
  - version-file (string): Path to the file containing version string

  Returns:
  - string: The version string read from the file"
  [version-file]
  (when version-file
    (try
      (-> version-file
          slurp
          str/trim)
      (catch Exception e
        (throw (ex-info (format "Failed to read version from file: %s" version-file)
                        {:version-file version-file}
                        e))))))

(defn- get-version
  "Gets the version string for the library.
  
  Parameters:
  - version (string): The base version number from parameters
  - version-file (string): Optional path to file containing version
  - snapshot (boolean): Whether this is a snapshot version
  
  Returns:
  - string: The complete version string with template variables replaced and optional SNAPSHOT suffix"
  [{:keys [version version-file snapshot]}]
  (let [version-from-file (read-version-from-file version-file)
        effective-version (or version-from-file version)
        processed-version (process-version-template effective-version)
        new-version (if (true? snapshot)
                      (str processed-version SNAPSHOT-SUFFIX)
                      processed-version)]
    (println (format "New version: %s" new-version))
    new-version))

(def default-license
  [:licenses
   [:license
    [:name "MIT License"]
    [:url "https://opensource.org/license/mit"]]])

(defn- get-license
  "Gets the license information for the library.
  
  Parameters:
  - license (map): A map containing the :name and :url keys for the license
  
  Returns:
  - vector: The license information in POM format, using the default MIT license if none provided"
  [license]
  (if (seq license)
    [:licenses
     [:license
      [:name (:name license)]
      [:url (:url license)]]]
    default-license))

(defn- pom-template
  "Generate a template for POM file data.

  Parameters:
  - url (string): The project URL
  - description (string): Project description
  - developer (string): Developer name
  - license (map): License information with :name and :url keys
  
  Returns:
  - vector: POM data in vector format with optional elements based on provided parameters"
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
  (when url
    (let [url-no-protocol (str/replace-first url #"^https://" "")]
      {:url url
       :connection (format "scm:git:git://%s.git" url-no-protocol)
       :developerConnection (format "scm:git:ssh://git@%s.git" url-no-protocol)})))

(defn- git-sha-latest
  []
  (b/git-process {:git-args ["rev-parse" "HEAD"]}))

(defn- get-scm
  "Gets the SCM (Source Control Management) information for the library.
  
  Parameters:
  - url (string): The project URL
  - scm-url (string): Optional SCM-specific URL for SCM configuration, in case it differs with url
  - scm (map): Optional SCM configuration map containing :url, :connection, and :developerConnection
  - version (string): The version string
  - snapshot (boolean): Whether this is a snapshot version
  
  Returns:
  - map: The SCM information including connection details and version tag. 
        For snapshots, uses git SHA as tag. For releases, uses version as tag.
        If no SCM info provided, generates default from URL."
  [{:keys [url scm-url scm version snapshot]}]
  (let [url* (or scm-url url)
        tag (if snapshot
              (git-sha-latest)
              version)]
    (merge {:tag tag} (default-scm url*) scm)))

(defn- parse-params
  [{:keys [lib
           target-dir
           jar-file
           src-dirs
           resource-dirs
           class-dir
           scm
           pom-data
           ; custom params
           url
           scm-url
           basis-params
           snapshot]
    :or {snapshot false
         basis-params {:project "deps.edn"}}
    :as params}]
  (s/assert ::params params)
  (let [effective-version (get-version params)
        target-dir* (or target-dir TARGET-DIR)]
    (-> params
        (dissoc :version :snapshot :basis-params :url :license :version-file)
        (assoc
          :version effective-version
          :jar-file (or jar-file (format "%s/%s-%s.jar" target-dir* lib effective-version))
          :basis (b/create-basis basis-params)
          :target-dir target-dir*
          :class-dir (or class-dir (format "%s/classes" target-dir*))
          :src-dirs (or src-dirs ["src"])
          :resource-dirs (or resource-dirs ["resources"])
          :scm (get-scm {:url url
                         :scm-url scm-url
                         :scm scm
                         :version effective-version
                         :snapshot snapshot})
          :pom-data (or pom-data (pom-template params))))))

; Public API

(defn build
  "Builds a jar file for the library.

  Parameters:
  - params (map): A map containing build configuration parameters"
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
  "Builds and installs the jar file to the local repository.

  Parameters:
  - params (map): A map containing build configuration parameters"
  [params]
  (-> params
      (build)
      (b/install))
  (println "JAR has been installed to local repo successfully!"))

(defn deploy
  "Builds and deploys the jar file to Clojars.

  Parameters:
  - params (map): A map containing build and deployment configuration parameters"
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

(defn tag
  "Creates a git tag for the library version.

  Parameters:
  - version (string): The version to tag
  - push (boolean): Whether to push the tag to the remote repository (default: false)
  - msg (string): The optional message for the tag (default: 'Release version X.Y.Z')"
  [{:keys [push msg]
    :as params}]
  (let [effective-version (-> params
                              (dissoc :snapshot)
                              (get-version))]
    (b/git-process
      (let [msg (or msg (format "'Release version %s'" effective-version))]
        {:git-args ["tag" "-a" effective-version "-m" msg]}))
    (when (true? push)
      (b/git-process {:git-args ["push" "origin" effective-version]}))))
