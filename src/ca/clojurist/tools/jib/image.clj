(ns ca.clojurist.tools.jib.image
  "Defines the various types of images that can be created."
  {:author "Robert Medeiros" :email "robert@clojurist.ca"}
  (:import
   [java.io File]
   [java.nio.file Path])
  (:require
   [taoensso.truss :refer [have?]])
  (:require
   [ca.clojurist.tools.impl.jib.image.docker :as image.docker]
   [ca.clojurist.tools.impl.jib.image.registry :as image.registry]
   [ca.clojurist.tools.impl.jib.image.tar :as image.tar]))

(set! *warn-on-reflection* true)

;; tar-image
;; -------------------------------------------------------------------------------------------------

(defn tar-image
  "Generates a tarball image from a build plan.

  Options:
  * `image-name` - required, the name that is used when the tarball is imported
  * `path` - required, the output location of the tarball file"
  [image-name path]
  {:pre [(have? string? image-name)
         (have? (or (string? path)
                    (instance? File path)
                    (instance? Path path)))]}
  (image.tar/tar-image image-name path))

;; registry-image
;; -------------------------------------------------------------------------------------------------

(defn registry-image
  "Generates a registry image from a build plan."
  [image-name]
  {:pre [(have? string? image-name)]}
  (image.registry/registry-image image-name))

(defn registry-image?
  "Returns true if the provided value is a map representing a registry image under construction. Returns false otherwise."
  [x]
  (image.registry/registry-image? x))

(defn set-credentials
  "Set the username and password to use when authenticating with a container image registry."
  [image-map username password]
  {:pre [(have? registry-image? image-map)
         (every? string? [username password])]}
  (image.registry/set-credentials image-map username password))

(defn use-retriever
  "Provide a 'retriever' function that returns the credentials to use to authenticate with a container image registry. The function must return a vector containing the username and password as strings, i.e. `[username password]`."
  [image-map retriever-fn]
  {:pre [(have? registry-image? image-map)
         (have? fn? retriever-fn)]}
  (image.registry/use-retriever image-map retriever-fn))

(defn use-docker-config
  "Use credentials for a container image registry that are taken from Docker configuration.

  CONFIG OPTIONS:
  - `:docker/legacy?` - (optional, default: false) attempt to load legacy Docker credentials

  - `:docker/config-file` - (optional) a string, File, or Path pointing at a Docker configuration file
  - `:docker/environment` - (optional) a map of environment variables to use as Docker environemnt"
  ([image-map]
   {:pre [(have? registry-image? image-map)]}
   (image.registry/use-docker-config image-map))

  ([image-map config]
   {:pre [(have? registry-image? image-map)
          (have? map? config)]}
   (image.registry/use-docker-config image-map config)))

(defn use-google-credentials
  "Use credentials for a container image registry that are taken from Google Application Default Credentials."
  [image-map]
  {:pre [(have? registry-image? image-map)]}
  (image.registry/use-google-credentials image-map))

(defn use-well-known-helpers
  "Attempt to find registry credentials using one of several well-known configuration helpers."
  [image-map]
  {:pre [(have? registry-image? image-map)]}
  (image.registry/use-well-known-helpers image-map))

;; docker-image
;; -------------------------------------------------------------------------------------------------

(defn docker-image
  "Generates a docker daemon image from a build plan.

  Arguments:
  * `image-name` - required, an image reference used to tag the built image
  * `docker-config` - optional, a Docker configuration map containing keys:
    * `:docker/executable` - optional, path to Docker binary, defaults to `docker`
    * `:docker/environment` - optional, a map of environment variables to set when running `docker`"
  ([image-name]
   {:pre [(have? string? image-name)]}
   (image.docker/docker-image image-name))

  ([image-name docker-config]
   {:pre [(have? string? image-name)
          (have? map? docker-config)]}
   (image.docker/docker-image image-name docker-config)))
