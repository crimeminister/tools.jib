(ns ca.clojurist.tools.impl.jib.image.docker
  "Implementation of support for generating Docker daemon images."
  {:author "Robert Medeiros" :email "robert@clojurist.ca"}
  (:import
   [com.google.cloud.tools.jib.api
    Containerizer
    DockerClient
    DockerDaemonImage
    ImageReference]
   [com.google.cloud.tools.jib.docker
    CliDockerClient
    DockerClientResolver])
  (:require
   [clojure.java.io :as io])
  (:require
   [ca.clojurist.tools.impl.jib.image :as jib.image]))

(set! *warn-on-reflection* true)

;; -------------------------------------------------------------------------------------------------

(def docker-executable
  "The default Docker client."
  CliDockerClient/DEFAULT_DOCKER_CLIENT)

;; docker-image
;; -------------------------------------------------------------------------------------------------

(defn docker-image
  ""
  ([image-name]
   {:jib/type :jib.type/docker-image
    :jib/image-name image-name})

  ([image-name docker-config]
   (let [executable (get docker-config :docker/executable)
         environment (get docker-config :docker/environment)
         parameters (get docker-config :docker/parameters)]
     (cond-> {:jib/type :jib.type/docker-image
              :jib/image-name image-name}
       ;; :docker/executable
       (some? executable)
       (assoc :jib.docker/executable executable)
       ;; :docker/environment
       (some? environment)
       (assoc :jib.docker/environment environment)
       ;; :docker/parameters
       (some? parameters)
       (assoc :jib.docker/parameters parameters)))))

(defmethod jib.image/containerizer :jib.type/docker-image
  [{:keys [jib/image-name] :as image}]
  (let [image-ref (ImageReference/parse image-name)
        docker-env (get image :jib.docker/environment)
        docker-exe (some-> (get image :jib.docker/executable)
                           io/file
                           .toPath)
        docker-params (get image :jib.docker/parameters)
        docker-img (cond-> (DockerDaemonImage/named image-ref)
                     ;; :jib.docker/environment
                     (some? docker-env)
                     (.setDockerEnvironment docker-env)
                     ;; :jib.docker/executable
                     (some? docker-exe)
                     (.setDockerExecutable docker-exe))
        ;; Any parameters needed by the Docker client.
        docker-params (if (some? docker-params)
                        docker-params
                        (java.util.HashMap.))
        ;; Returns a java.util.Optional with an .isPresent() method to check whether as result is
        ;; available or not.
        docker-client (DockerClientResolver/resolve docker-params)]
    #_(Containerizer/to ^DockerDaemonImage docker-img)
    (if (.isPresent docker-client)
      (Containerizer/to ^DockerClient docker-client ^DockerDaemonImage docker-img)
      (throw (ex-info "No Docker client found" {})))))
