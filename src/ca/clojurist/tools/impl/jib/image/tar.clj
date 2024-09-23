(ns ca.clojurist.tools.impl.jib.image.tar
  "Implementation of support for generating tarball container images."
  {:author "Robert Medeiros" :email "robert@clojurist.ca"}
  (:import
   [java.io File]
   [java.nio.file Path])
  (:import
   [com.google.cloud.tools.jib.api
    Containerizer
    ImageReference
    TarImage])
  (:require
   [clojure.java.io :as io])
  (:require
   [taoensso.truss :refer [have]])
  (:require
   [ca.clojurist.tools.impl.jib.image :as jib.image]))

(set! *warn-on-reflection* true)

;; tar-image
;; -------------------------------------------------------------------------------------------------

(defn tar-image
  ""
  [image-name path]
  (let [;; The API requires a java.nio.file.Path, convert what we're given to that.
        path (cond
               ;; String
               (string? path)
               (.toPath (io/file path))
               ;; File
               (instance? File path)
               (.toPath ^File path)
               ;; Path
               (instance? Path path)
               path)]
    {:jib/type :jib.type/tar-image
     :jib/image-name image-name
     :jib/path path}))

(defmethod jib.image/containerizer :jib.type/tar-image
  [{:keys [jib/image-name jib/path]}]
  (let [image-name (have string? image-name)
        path (have (or (string? path)
                       (instance? File path)
                       (instance? Path path)))
        image-ref (ImageReference/parse image-name)
        tar-img (-> (TarImage/at path) (.named image-ref))]
    (Containerizer/to tar-img)))
