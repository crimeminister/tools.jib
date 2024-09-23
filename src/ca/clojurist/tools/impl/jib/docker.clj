(ns ca.clojurist.tools.impl.jib.docker
  "Implementation of Docker-related functionality."
  {:author "Robert Medeiros" :email "robert@clojurist.ca"}
  (:import
   [com.google.cloud.tools.jib.api DockerClient]))

(set! *warn-on-reflection* true)

;; -------------------------------------------------------------------------------------------------

(defn supported?
  "Validate if the Docker client is supported."
  [parameters]
  :todo
  )

(defn load!
  "Loads an image tarball into the Docker daemon."
  [tarball listener]
  :todo
  )

(defn save!
  "Saves an image tarball from the Docker daemon."
  [image-name output-path listener]
  :todo
  )

(defn inspect
  "Gets the size, image ID, and diff IDs of an image in the Docker daemon."
  [image-name]
  :todo
  )
