(ns ca.clojurist.tools.impl.jib.image.registry
  "Implementation of support for generating container registry images."
  {:author "Robert Medeiros" :email "robert@clojurist.ca"}
  (:import
   [java.nio.file Path]
   [java.util Optional])
  (:import
   [com.google.cloud.tools.jib.api
    Containerizer
    Credential
    CredentialRetriever
    ImageReference
    RegistryImage]
   [com.google.cloud.tools.jib.frontend
    CredentialRetrieverFactory])
  (:require
   [taoensso.truss :refer [have]])
  (:require
   [ca.clojurist.tools.impl.jib.image :as jib.image]
   [ca.clojurist.tools.impl.jib.util :as jib.util]))

(set! *warn-on-reflection* true)

;; registry-image
;; -------------------------------------------------------------------------------------------------

(defn registry-image
  "Returns a map that is meant to be threaded through additional functions in order to add
  configuration that describes the registry image to create, the credentials to use when publishing
  the image, etc."
  [image-name]
  {:jib/type :jib.type/registry-image
   :jib/image-name image-name})

(defn registry-image?
  [x]
  (and
   (map? x)
   (= :jib.type/registry-image (get x :jib/type))))

(defn set-credentials
  ""
  [image-map username password]
  (assoc image-map
         :jib/credential-source ::provided
         :jib/username username
         :jib/password password))

(defn use-retriever
  ""
  [image-map retriever-fn]
  (if-not (fn? retriever-fn)
    (throw (ex-info "retriever is not a function" {:retriever retriever-fn}))
    (assoc image-map
           :jib/credential-source ::function
           :jib/retriever retriever-fn)))

(defn use-docker-config
  ""
  ([image-map]
   (use-docker-config image-map {}))

  ([image-map {:keys [docker/legacy? docker/config-file docker/environment]}]
   (let [credential-source (if legacy?
                             ::docker-legacy
                             ::docker)
         config-path (jib.util/file->path config-file)
         image-map (merge image-map {:jib/credential-source credential-source})]
     (when (and legacy? (not (instance? Path config-path)))
       (throw (ex-info "path to legacy Docker config file is required"
                       {:config-path config-path})))
     (cond-> image-map
       ;; User supplied a path to a configuration file.
       (some? config-path)
       (assoc :docker/config-path config-path)
       ;; Use supplied a map of environment variables to use as Docker environment.
       (some? environment)
       (assoc :docker/environment environment)))))

(defn use-google-credentials
  ""
  [image-map]
  (assoc image-map :jib/credential-source ::google))

(defn use-well-known-helpers
  ""
  [image-map]
  (assoc image-map :jib/credential-source ::well-known))

(defn- credential-retriever-factory
  ^CredentialRetrieverFactory [{:keys [jib/image-name docker/environment]}]
  (let [image-name (have string? image-name)
        image-ref (ImageReference/parse image-name)
        ;; TODO support better logging (telemere)
        ;; logger-fn! (fn [^LogEvent event]
        ;;              (prn :fixme/logging))
        ;; TODO Clojure 1.12 should auto-convert to Consumer, but it isn't working the way I have
        ;; attempted above!
        logger-fn! (reify java.util.function.Consumer
                     (accept [_ event]
                       (prn event)))
        environment-map (when (map? environment)
                          (java.util.HashMap. ^java.util.Map environment))]
    (cond
      (every? some? [logger-fn! environment-map])
      (CredentialRetrieverFactory/forImage image-ref logger-fn! environment-map)

      (some? logger-fn!)
      (CredentialRetrieverFactory/forImage image-ref logger-fn!)

      :else
      (throw (ex-info "unable to construct CredentialRetrieverFactory"
                      {})))))

(defn- add-credential
  ""
  [registry-img {:keys [jib/username jib/password]}]
  {:post [(instance? RegistryImage %)]}
  (if-not (every? string? [username password])
    (throw (ex-info "invalid credentials" {}))
    (.addCredential ^RegistryImage registry-img username password)))

(defn- add-credential-retriever
  "Add a CredentialRetriever to the provided RegistryImage instance. The provided function is wrapped
  to implement the CredentialRetriever interface, and should return container registry credentials
  of the form `[username password]`."
  [{:keys [jib/retriever]}]
  (let [retriever-fn (have fn? retriever)]
    (reify CredentialRetriever
      (retrieve [_]
        ;; Invoke the provided function to retrieve credentials, then return them as an
        ;; Optional<Credential>. The provided retriever function must return the credentials as a
        ;; vector of [username password].
        (if-let [[username password] (retriever-fn)]
          (let [credential (Credential/from username password)]
            (Optional/of credential))
          (Optional/empty))))))

(defn- docker-config
  "Return a CredentialRetriever that attempts to retrieve registry credentials from Docker
  configuration."
  [{:keys [jib/docker-path] :as image-config}]
  (let [crf (credential-retriever-factory image-config)]
    (if (instance? Path docker-path)
      (.dockerConfig crf docker-path)
      (.dockerConfig crf))))

(defn- docker-legacy-config
  "Return a CredentialRetriever that attempts to retrieve registry credentials from legacy Docker
  configuration."
  [{:keys [jib/docker-path] :as image-config}]
  (let [crf (credential-retriever-factory image-config)]
    ;; The path to the legacy Docker config file is required.
    (.legacyDockerConfig crf docker-path)))

(defn- google-application-default
  "Return a CredentialRetriever that attempts to retrieve registry credentials from Google Application
  Default Credentials."
  ^CredentialRetriever [image-config]
  (let [crf (credential-retriever-factory image-config)]
    (.googleApplicationDefaultCredentials crf)))

(defn- well-known-helpers
  "Return a CredentialRetriever that attempts to retrieve registry credentials using several
  well-known Docker credential helpers."
  [image-config]
  (let [crf (credential-retriever-factory image-config)]
    (.wellKnownCredentialHelpers crf)))

(defn- add-retriever
  [^RegistryImage registry-img {:keys [jib/credential-source] :as image-config}]
  {:post [(instance? RegistryImage %)]}
  (let [credential-retriever
        (condp = credential-source
          ;; The retriever is a function that returns registry username/password.
          ::function add-credential-retriever
          ;; Attempt to retrieve registry credentials from Docker config.
          ::docker docker-config
          ;; Attempt to retrieve registry credentials from legacy Docker config.
          ::docker-legacy docker-legacy-config
          ;; Attempt to retrieve registry credentials from Google Application Default Credentials.
          ::google google-application-default
          ;; Try to get registry credentials using well-known helpers.
          ::well-known well-known-helpers)
        ;; Use a factory to build a CredentialRetriever.
        retriever (credential-retriever image-config)]
    (.addCredentialRetriever registry-img retriever)))

(defmethod jib.image/containerizer :jib.type/registry-image
  [{:keys [jib/image-name jib/credential-source] :as image-config}]
  (let [image-ref (ImageReference/parse image-name)
        registry-img (cond-> (RegistryImage/named image-ref)
                       ;; User has provided explicit username and password.
                       (contains? #{::provided} credential-source)
                       (add-credential image-config)
                       ;; User specified a means of obtaining credentials that requires using a
                       ;; CredentialRetriever.
                       (contains? #{::function
                                    ::docker
                                    ::docker-legacy
                                    ::google
                                    ::well-known}
                                  credential-source)
                       (add-retriever image-config))]
    (Containerizer/to ^RegistryImage registry-img)))
