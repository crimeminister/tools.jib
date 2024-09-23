(ns ca.clojurist.tools.jib-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]])
  (:require
   [ca.clojurist.tools.jib :as jib]
   [ca.clojurist.tools.jib.plan :as jib.plan]))


(deftest from-test
  (testing "create a plan using a base image"
    (let [plan (jib/from "busybox")]
      (is (jib/plan? plan)
          "the result of jib/from must be a container build plan")
      (is (= :jib.type/plan (get plan :jib/type))
          "invalid plan type tag")
      (is (= "busybox" (get plan :jib/from))
          "base image name must be set correctly")
      (is (vector? (get plan :jib/plan))
          "the plan has a vector of steps")
      )))

(deftest plan-test
  (letfn [;; Return the most recently added step from a build plan.
          (get-step [plan]
            (-> plan :jib/plan last))
          ;; Return the keyword type tag for a build step.
          (get-type [step]
            (get step :jib/type))]

    (let [plan (jib/from "busybox")]

      (testing "add-env-var"
        (let [plan (jib.plan/add-env-var plan "FOO" "BAR")
              step (get-step plan)]
          (is (map? step)
              "every plan step must be a map")
          (is (= :jib.type/env-var (get-type step))
              "step must have the correct type tag")
          (is (= "FOO" (get step :jib/name))
              "the environment variable name must match")
          (is (= "BAR" (get step :jib/value))
              "the environment variable value must match")))

      (testing "add-exposed-port"
        (doseq [protocol #{:protocol/tcp :protocol/udp}]
          (testing (str protocol)
            (let [port 8080
                  plan (jib.plan/add-exposed-port plan protocol port)
                  step (get-step plan)]
              (is (map? step)
                  "every plan step must be a map")
              (is (= :jib.type/exposed-port (get-type step))
                  "step must have the correct type tag")
              (is (= protocol (get step :jib/protocol))
                  "the protocol must match")
              (is (= port (get step :jib/port))
                  "the port must match")))))

      (testing "add-label"
        (let [label "AUTHOR"
              value "Bozo"
              plan (jib.plan/add-label plan label value)
              step (get-step plan)]
          (is (map? step)
              "every plan step must be a map")
          (is (= :jib.type/label (get-type step))
              "step must have the correct type tag")
          (is (= label (get step :jib/key))
              "the label name must match")
          (is (= value (get step :jib/value))
              "the label value must match")))

      (testing "add-layer"
        (let [;; Files to add to the layer are specified as strings or Paths.
              files ["README.md" (-> "CHANGELOG" io/file .toPath)]
              ;; Destination container path can be a string or Path.
              path (-> "/tmp" io/file .toPath)
              plan (jib.plan/add-layer plan files path)
              step (get-step plan)]
          (is (map? step)
              "every plan step must be a map")
          (is (= :jib.type/layer (get-type step))
              "step must have the correct type tag")
          (is (= files (get step :jib/files))
              "the layer files must match")
          (is (= path (get step :jib/path))
              "the container path must match")))

      (testing "add-platform"
        (let [arch "x86_64"
              os "Linux"
              plan (jib.plan/add-platform plan arch os)
              step (get-step plan)]
          (is (map? step)
              "every plan step must be a map")
          (is (= :jib.type/platform (get-type step))
              "step must have the correct type tag")
          (is (= arch (get step :jib/architecture))
              "the architecture must match")
          (is (= os (get step :jib/os))
              "the operating system must match")))

      (testing "add-volume"
        (let [volume-dir "/mnt"
              plan (jib.plan/add-volume plan volume-dir)
              step (get-step plan)]
          (is (map? step)
              "every plan step must be a map")
          (is (= :jib.type/volume (get-type step))
              "step must have the correct type tag")
          (is (= volume-dir (get step :jib/dir))
              "the volume directory must match")))

      (testing "set-arguments"
        (let [arguments ["arg1" "arg2"]
              plan (jib.plan/set-arguments plan arguments)
              step (get-step plan)]
          (is (map? step)
              "every plan step must be a map")
          (is (= :jib.type/arguments (get-type step))
              "step must have the correct type tag")
          (is (= arguments (get step :jib/arguments))
              "the container arguments must match")))

      (testing "set-creation-time"
        (let [creation-time #inst "1970-01-01T00:00:00Z"
              plan (jib.plan/set-creation-time plan creation-time)
              step (get-step plan)]
          (is (map? step)
              "every plan step must be a map")
          (is (= :jib.type/creation-time (get-type step))
              "step must have the correct type tag")
          (is (= creation-time (get step :jib/time))
              "the creation time must match")))

      (testing "set-entrypoint"
        (let [entrypoint ["do" "the" "thing"]
              plan (jib.plan/set-entrypoint plan entrypoint)
              step (get-step plan)]
          (is (map? step)
              "every plan step must be a map")
          (is (= :jib.type/entrypoint (get-type step))
              "step must have the correct type tag")
          (is (= entrypoint (get step :jib/entrypoint))
              "the entrypoint must match")))

      (testing "set-format"
        (doseq [format #{:image-format/docker :image-format/oci}]
          (let [plan (jib.plan/set-format plan format)
                step (get-step plan)]
            (is (map? step)
                "every plan step must be a map")
            (is (= :jib.type/format (get-type step))
                "step must have the correct type tag")
            (is (= format (get step :jib/format))
                "the container image format must match"))))

      (testing "set-platforms"
        (let [;; A set of support platforms, each with an architecture and OS.
              platforms #{{:jib/arch "amd64" :jib/os "linux"}}
              plan (jib.plan/set-platforms plan platforms)
              step (get-step plan)]
          (is (map? step)
              "every plan step must be a map")
          (is (= :jib.type/platform-set (get-type step))
              "step must have the correct type tag")
          (is (= platforms (get step :jib/platforms))
              "the platform set must match")))

      (testing "set-user"
        (let [user "robert"
              plan (jib.plan/set-user plan user)
              step (get-step plan)]
          (is (map? step)
              "every plan step must be a map")
          (is (= :jib.type/user (get-type step))
              "step must have the correct type tag")
          (is (= user (get step :jib/user))
              "the user must match")))

      (testing "set-work-dir"
        (let [work-dir "/tmp"
              plan (jib.plan/set-work-dir plan work-dir)
              step (get-step plan)]
          (is (map? step)
              "every plan step must be a map")
          (is (= :jib.type/work-dir (get-type step))
              "step must have the correct type tag")
          (is (= work-dir (get step :jib/dir))
              "the work directory must match"))))))
