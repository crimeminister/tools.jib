(ns ca.clojurist.tools.impl.jib
  "Implementation of jib wrapper."
  {:author "Robert Medeiros" :email "robert@clojurist.ca"}
  (:import
   [com.google.cloud.tools.jib.api
    Containerizer
    ImageReference
    Jib
    JibContainer
    JibContainerBuilder])
  (:require
   [ca.clojurist.tools.impl.jib.plan :as impl.plan]
   [ca.clojurist.tools.impl.jib.image :as impl.image]))

(set! *warn-on-reflection* true)

;; plan?
;; -------------------------------------------------------------------------------------------------

(defn plan?
  ""
  [x]
  (and
   (map? x)
   (= :jib.type/plan (get x :jib/type))))

;; from
;; -------------------------------------------------------------------------------------------------

(defn from
  ""
  [^String base]
  {:jib/type :jib.type/plan
   :jib/from base
   :jib/plan []})

(comment
  (from "busybox")
  )

;; read-edn
;; -------------------------------------------------------------------------------------------------

;; TODO read plan from string containing edn build plan.
#_(defn read-edn
  ""
  [^String s]
  :todo)

;; write-edn
;; -------------------------------------------------------------------------------------------------

;; TODO write plan as a string containing edn.
#_(defn write-edn
  ""
  [plan]
  :todo)

;; containerize
;; -------------------------------------------------------------------------------------------------
;; TODO DockerClient + DockerDaemonImage
;; TODO CredentialRetriever / CredentialRetrieverFactory

(defn containerize
  ""
  ^JibContainer [{:keys [jib/from jib/plan]} target]
  (let [ ;; Throws if given invalid base image reference.
        image-ref (ImageReference/parse from)
        ;; The accumulator of the reducer is a `JibContainerBuilder`. Reduce over the steps in the
        ;; container build plan (each is a map) using the plan-step multimethod. This dispatches on
        ;; the value of :jib/type to call the appropriate method on the JibContainerBuilder, passing
        ;; in the arguments for the step that are contained in the step map.
        jcb (reduce (fn [jcb step]
                      (impl.plan/plan-step jcb step))
                    ;; Returns a JibContainerBuilder.
                    (Jib/from image-ref)
                    ;; A vector maps describing each build plan step.
                    plan)
        containerizer (impl.image/containerizer target)]
    (.containerize ^JibContainerBuilder jcb ^Containerizer containerizer)))
