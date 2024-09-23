(ns ca.clojurist.tools.jib.docker
  "Interact with a Docker daemon."
  {:author "Robert Medeiros" :email "robert@clojurist.ca"}
  (:import
   [java.nio.file Path])
  (:require
   [ca.clojurist.tools.impl.jib.docker :as impl]))

(set! *warn-on-reflection* true)

;; -------------------------------------------------------------------------------------------------

(defn supported?
  "Validate if the Docker client is supported."
  [parameters]
  {:pre [(map? parameters)]
   :post [(boolean? %)]}
  (impl/supported? parameters))

(defn load!
  "Loads an image tarball into the Docker daemon."
  [tarball listener]
  {:pre []
   :post [(string? %)]}
  (impl/load! tarball listener))

(defn save!
  "Saves an image tarball from the Docker daemon."
  [image-name output-path listener]
  {:pre [(string? image-name)
         (instance? Path output-path)
         (fn? listener)]
   :post []}
  (impl/save! image-name output-path listener))

(defn inspect
  "Gets the size, image ID, and diff IDs of an image in the Docker daemon."
  [image-name]
  {:pre [(string? image-name)]
   :post []}
  (impl/inspect image-name))
