(ns ca.clojurist.tools.jib.plan
  "Functions for generating steps in a container build plan."
  {:author "Robert Medeiros" :email "robert@clojurist.ca"}
  (:import
   [java.nio.file Path])
  (:require
   [ca.clojurist.tools.impl.jib.plan :as impl]))

(set! *warn-on-reflection* true)

;; -------------------------------------------------------------------------------------------------

(defn add-env-var
  "Set an environment variable in the container."
  [plan ^String k ^String v]
  {:pre [(map? plan)
         (every? string? [k v])]}
  (impl/add-env-var plan k v))

(defn add-exposed-port
  "Exposes a port from the container."
  [plan protocol port]
  {:pre [(map? plan)
         (contains? #{:protocol/tcp :protocol/udp} protocol)
         (pos-int? port)]}
  (impl/add-exposed-port plan protocol port))

(defn add-label
  "Sets a label for the container with name `key` and value `value`."
  [plan key value]
  {:pre [(map? plan)
         (every? string? [key value])]}
  (impl/add-label plan key value))

(defn add-layer
  "Adds a new layer to the container with `files` as source files and `path` as the target path in the
  container."
  [plan files path]
  {:pre [(map? plan)
         (every? #(or (string? %) (instance? Path %)) files)
         (or (string? path) (instance? Path path))]}
  (impl/add-layer plan files path))

(defn add-platform
  "Adds a desired image platform (os and architecture pair)."
  [plan architecture os]
  {:pre [(map? plan)
         (every? string? [architecture os])]}
  (impl/add-platform plan architecture os))

(defn add-volume
  "Sets a directory that may be used to host an external volume."
  [plan volume-dir]
  {:pre [(map? plan)
         (or (string? volume-dir) (instance? Path volume-dir))]}
  (impl/add-volume plan volume-dir))

(defn set-arguments
  "Sets the container entrypoint program arguments."
  [plan arguments]
  {:pre [(map? plan)
         (vector? arguments)
         (every? string? arguments)]}
  (impl/set-arguments plan arguments))

(defn set-creation-time
  "Sets the creation time of the container."
  [plan creation-time]
  {:pre [(map? plan)
         (inst? creation-time)]}
  (impl/set-creation-time plan creation-time))

(defn set-entrypoint
  "Sets the container entrypoint."
  [plan entrypoint]
  {:pre [(map? plan)
         (vector? entrypoint) (every? string? entrypoint)]}
  (impl/set-entrypoint plan entrypoint))

(defn set-format
  "Sets the format of the container image.

  Parameters:
  * `format` - one of `:image-format/docker`, `:image-format/oci`"
  [plan format]
  {:pre [(map? plan)
         (contains? #{:image-format/oci :image-format/docker} format)]}
  (impl/set-format plan format))

(defn set-platforms
  "Sets the list of desired platforms."
  [plan platforms]
  {:pre [(map? plan)
         (set? platforms)
         (every? map? platforms)]}
  (impl/set-platforms plan platforms))

(defn set-user
  "Set the user and group to run the container as."
  [plan user]
  {:pre [(map? plan)
         (string? user)]}
  (impl/set-user plan user))

(defn set-work-dir
  "Sets the working directory in the container."
  [plan work-dir]
  {:pre [(map? plan)
         (or (string? work-dir) (instance? Path work-dir))]}
  (impl/set-work-dir plan work-dir))
