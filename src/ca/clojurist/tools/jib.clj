(ns ca.clojurist.tools.jib
  "A wrapper around the Jib container build tool."
  {:author "Robert Medeiros" :email "robert@clojurist.ca"}
  (:require
   [ca.clojurist.tools.impl.jib :as impl]))

(set! *warn-on-reflection* true)

;; -------------------------------------------------------------------------------------------------
;; TODO truss, assertions
;; TODO define edn configuration using malli, data readers to define an image

(defn plan?
  "Return true if `x` is a jib build plan, and false otherwise."
  [x]
  (impl/plan? x))

(defn from
  "Initialize a new build plan using the given base image."
  [base]
  {:pre [(string? base)]}
  (impl/from base))

#_(defn read-edn
  "Parse an edn build plan from a string return it."
  [s]
  {:pre [(string? s)]}
  (impl/read-edn s))

#_(defn write-edn
  "Write a build plan as an edn string."
  [plan]
  {:pre [(plan? plan)]}
  (:impl/write-edn plan))

(defn containerize
  "Builds the container according to the given build `plan`."
  [plan image]
  {:pre [(plan? plan)]}
  (impl/containerize plan image))
