(ns ca.clojurist.tools.impl.jib.image
  "Implementation of container image generation functionality."
  {:author "Robert Medeiros" :email "robert@clojurist.ca"})

(set! *warn-on-reflection* true)

;; -------------------------------------------------------------------------------------------------

(defmulti containerizer
  "A Jib Containerizer performs the work of turning a build plan into some kind of container
  image. Each target type is defined by a map produced by one of the functions in the `jib.image`
  namespace; the type is indicated by the value of the `:jib/type` key in the map, which we dispatch
  on. Each implementation converts the configuration for that type of container target into a
  `Containerizer` instance."
  :jib/type)
