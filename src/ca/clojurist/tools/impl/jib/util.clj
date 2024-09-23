(ns ca.clojurist.tools.impl.jib.util
  "Miscellaneous internal utilities."
  {:author "Robert Medeiros" :email "robert@clojurist.ca"}
  (:import
   [java.io File]
   [java.nio.file Path])
  (:require
   [clojure.java.io :as io]))

(set! *warn-on-reflection* true)

;; -------------------------------------------------------------------------------------------------

(defn file->path
  "Convert a pointer at a file into a java.nio.file.Path. It will convert a String or File, or return
  a Path unchanged. Any other argument will result in `nil` being returned."
  [x]
  (cond
    ;; Path
    (instance? Path x)
    x
    ;; File
    (instance? File x)
    (.toPath ^File x)
    ;; String
    (string? x)
    (-> x io/file .toPath)))
