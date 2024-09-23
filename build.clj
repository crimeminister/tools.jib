(ns build
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.tools.build.api :as b]))

(set! *warn-on-reflection* true)

;; ---------------------------------------------------------

(def src-dirs
  "Locations of project source code."
  ["src" "resources"])

(def target-dir
  "The parent directory for all build output."
  (str (io/file "target")))

(def class-dir
  "The location of compiled Java class files."
  (str (io/file target-dir "classes")))

(def uberjar-filename
  "The filename of the uberjar file containing all dependencies."
  "tools.jib-standalone.jar")

(def uberjar-file
  "The path to the uberjar file to build."
  (str (io/file target-dir uberjar-filename)))

(def project-config
  "Project configuration to support all tasks."
  {:class-directory class-dir
   :main-namespace 'ca.clojurist/tools.jib
   :project-basis (b/create-basis)
   :uberjar-file uberjar-file})

;; config
;; ---------------------------------------------------------
;; Output project configuration.

(defn config
  "Display build configuration."
  [config]
  (pp/pprint (or config project-config)))

;; clean
;; ---------------------------------------------------------
;; By default, removes the build output directory.

(defn clean
  "Remove a directory.
  - `:path '\"directory-name\"'` for a specific directory
  - `nil` (or no command line arguments) to delete `target` directory
  `target` is the default directory for build artifacts.
  Checks that `.` and `/` directories are not deleted."
  [directory]
  (when (not (contains? #{"." "/"} directory))
    (b/delete {:path (or (:path directory) target-dir)})))

;; uberjar
;; ---------------------------------------------------------

(defn uberjar
  "Create an archive containing Clojure and the build of the project
  Merge command line configuration to the default project config"
  [options]
  (let [config (merge project-config options)
        {:keys [class-directory main-namespace project-basis uberjar-file]} config]
    ;; Remove old build output.
    (clean target-dir)
    ;; Include source files in uberjar.
    (b/copy-dir {:src-dirs src-dirs
                 :target-dir class-directory})
    ;; Compile source files.
    (b/compile-clj {:basis project-basis
                    :class-dir class-directory
                    :src-dirs src-dirs})
    ;; Assemble the uberjar.
    (b/uber {:basis project-basis
             :class-dir class-directory
             :main main-namespace
             :uber-file uberjar-file})))
