(ns ca.clojurist.tools.impl.jib.plan
  "Implementation of container build plans."
  {:author "Robert Medeiros" :email "robert@clojurist.ca"}
  (:import
   [com.google.cloud.tools.jib.api
    JibContainerBuilder]
   [com.google.cloud.tools.jib.api.buildplan
    AbsoluteUnixPath
    ImageFormat
    Platform
    Port])
  (:require
   [clojure.java.io :as io]))

(set! *warn-on-reflection* true)

;; -------------------------------------------------------------------------------------------------

(defmulti plan-step
  "This is a reducing function that runs of the container build plan, a sequence of maps where each
  map represents some configuration for the container. The type of build step is determined by the
  `:jib/type` tag in the map, which is what this multimethod dispatches on. Other values in the map
  are configuration for that specific type of operation, e.g. if setting a work directory in the
  container there will be a value containing the path of the work directory."
  (fn [_ step]
    (get step :jib/type)))

;; set-creation-time
;; -------------------------------------------------------------------------------------------------

(defn set-creation-time
  ""
  [plan creation-time]
  (update plan :jib/plan conj
          {:jib/type :jib.type/creation-time
           :jib/time creation-time}))

(defmethod plan-step :jib.type/creation-time
  [^JibContainerBuilder jcb {:keys [jib/time]}]
  (let [instant (cond
                  (instance? java.time.Instant time)
                  time
                  (instance? java.util.Date time)
                  (.toInstant ^java.util.Date time))]
    (.setCreationTime jcb ^java.time.Instant instant)))

(comment
  (set-creation-time {:jib/plan []} (java.time.Instant/now))
  )

;; add-env-var
;; -------------------------------------------------------------------------------------------------

(defn add-env-var
  ""
  [plan ^String k ^String v]
  (update plan :jib/plan conj
          {:jib/type :jib.type/env-var
           :jib/name k
           :jib/value v}))

(defmethod plan-step :jib.type/env-var
  [^JibContainerBuilder jcb step]
  (let [env-name (get step :jib/name)
        env-value (get step :jib/value)]
    (.addEnvironmentVariable jcb env-name env-value)))

(comment
  (add-env-var {:jib/plan []} "HOME" "/home/robert")
  )

;; add-exposed-port
;; -------------------------------------------------------------------------------------------------

(defn add-exposed-port
  ""
  [plan protocol port]
  (update plan :jib/plan conj
          {:jib/type :jib.type/exposed-port
           :jib/protocol protocol
           :jib/port port}))

(defmethod plan-step :jib.type/exposed-port
    [^JibContainerBuilder jcb {:keys [jib/protocol jib/port] :as step}]
    (let [port (condp = protocol
                 :protocol/tcp (Port/tcp port)
                 :protocol/udp (Port/udp port))]
      (.addExposedPort jcb ^Port port)))

(comment
  (add-exposed-port {:jib/plan []} :protocol/tcp 8080)
  (add-exposed-port {:jib/plan []} :protocol/udp 3000)
  )

;; add-label
;; -------------------------------------------------------------------------------------------------

(defn add-label
  ""
  [plan ^String key ^String value]
  (update plan :jib/plan conj
          {:jib/type :jib.type/label
           :jib/key key
           :jib/value value}))

(defmethod plan-step :jib.type/label
  [^JibContainerBuilder jcb {:keys [jib/key jib/value]}]
  (.addLabel jcb ^String key ^String  value))

(comment
  (add-label {:jib/plan []} "foo" "bar")
  )

;; add-layer
;; -------------------------------------------------------------------------------------------------

(defn add-layer
  ""
  [plan files ^String container-path]
  (update plan :jib/plan conj
          {:jib/type :jib.type/layer
           :jib/files files
           :jib/path container-path}))

(defmethod plan-step :jib.type/layer
  [^JibContainerBuilder jcb {:keys [jib/files jib/path]}]
  (let [files (mapv #(-> % io/file .toPath) files)
        container-path (-> path io/file .getCanonicalPath str AbsoluteUnixPath/get)]
    (.addLayer jcb ^java.util.List files ^AbsoluteUnixPath container-path)))

(comment
  (add-layer {:jib/plan []} ["README.org"] "/")
  (add-layer {:jib/plan []} ["README.org"] (-> "/" io/file .toPath))
  )

;; add-platform
;; -------------------------------------------------------------------------------------------------

(defn add-platform
  ""
  [plan ^String architecture ^String os]
  (update plan :jib/plan conj
          {:jib/type :jib.type/platform
           :jib/architecture architecture
           :jib/os os}))

(defmethod plan-step :jib.type/platform
  [^JibContainerBuilder jcb {:keys [jib/architecture jib/os]}]
  (.addPlatform jcb ^String architecture ^String  os))

(comment
  (add-platform {:jib/plan []} "x86_64" "Linux")
  )

;; add-volume
;; -------------------------------------------------------------------------------------------------

(defn add-volume
  ""
  [plan volume-dir]
  (update plan :jib/plan conj
          {:jib/type :jib.type/volume
           :jib/dir volume-dir}))

(defmethod plan-step :jib.type/volume
  [^JibContainerBuilder jcb {:keys [jib/dir]}]
  (let [volume-dir (-> dir io/file .getCanonicalPath str AbsoluteUnixPath/get)]
    (.addVolume jcb ^AbsoluteUnixPath volume-dir)))

(comment
  (add-volume {:jib/plan []} "/mnt")
  )

;; set-entrypoint
;; -------------------------------------------------------------------------------------------------

(defn set-entrypoint
  ""
  [plan entrypoint]
  (update plan :jib/plan conj
          {:jib/type :jib.type/entrypoint
           :jib/entrypoint entrypoint}))

(defmethod plan-step :jib.type/entrypoint
  [^JibContainerBuilder jcb {:keys [jib/entrypoint]}]
  (.setEntrypoint jcb ^java.util.List entrypoint))

(comment
  (set-entrypoint {:jib/plan []} ["sh", "/helloworld.sh"])
  )

;; set-format
;; -------------------------------------------------------------------------------------------------

(defn set-format
  ""
  [plan format]
  (update plan :jib/plan conj
          {:jib/type :jib.type/format
           :jib/format format}))

(defmethod plan-step :jib.type/format
  [^JibContainerBuilder jcb step]
  (let [image-format (condp = (get step :jib/format)
                       :image-format/docker ImageFormat/Docker
                       :image-format/oci ImageFormat/OCI)]
    ;; Returns a JibContainerBuilder.
    (.setFormat jcb ^ImageFormat image-format)))

(comment
  (set-format {:jib/plan []} :image-format/docker)
  (set-format {:jib/plan []} :image-format/oci)
  )

;; set-arguments
;; -------------------------------------------------------------------------------------------------

(defn set-arguments
  ""
  [plan arguments]
  (update plan :jib/plan conj
          {:jib/type :jib.type/arguments
           :jib/arguments arguments}))

(defmethod plan-step :jib.type/arguments
  [^JibContainerBuilder jcb {:keys [jib/arguments]}]
  (.setProgramArguments jcb ^java.util.List arguments))

(comment
  (set-arguments {:jib/plan []} ["foo" "bar"] )
  )

;; set-platforms
;; -------------------------------------------------------------------------------------------------

(defn set-platforms
  ""
  [plan platforms]
  (update plan :jib/plan conj
          {:jib/type :jib.type/platform-set
           :jib/platforms platforms}))

(defmethod plan-step :jib.type/platform-set
  [^JibContainerBuilder jcb {:keys [jib/platforms]}]
  (let [platforms (into #{} (map (fn [{:keys [jib/arch jib/os]}]
                                   (Platform. arch os))
                                 platforms)) ]
    (.setPlatforms jcb ^java.util.Set platforms)))

(comment
  (set-platforms {:jib/plan []} #{{:jib/arch "amd64" :jib/os "linux"}})
  )

;; set-user
;; -------------------------------------------------------------------------------------------------

(defn set-user
  ""
  [plan ^String user]
  (update plan :jib/plan conj
          {:jib/type :jib.type/user
           :jib/user user}))

(defmethod plan-step :jib.type/user
  [^JibContainerBuilder jcb {:keys [jib/user]}]
  (.setUser jcb ^String user))

(comment
  (set-user {:jib/plan []} "admin")
  )

;; set-work-dir
;; -------------------------------------------------------------------------------------------------

(defn set-work-dir
  ""
  [plan work-dir]
  (update plan :jib/plan conj
          {:jib/type :jib.type/work-dir
           :jib/dir work-dir}))

(defmethod plan-step :jib.type/work-dir
  [^JibContainerBuilder jcb {:keys [jib/dir]}]
  (let [work-dir (-> dir io/file .getCanonicalPath str AbsoluteUnixPath/get)]
    (.setWorkingDirectory jcb ^AbsoluteUnixPath work-dir)))

(comment
  (set-work-dir {:jib/plan []} "/tmp")
  )
