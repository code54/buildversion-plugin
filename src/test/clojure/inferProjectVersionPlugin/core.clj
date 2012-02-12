(ns inferProjectVersionPlugin.core
  (:use [clojure.test]
        [clojure.java.shell]))

(def git-project-dir (str maven-target-dir "/sample_project"))

(defn sample-git-project-fixture [run-tests-fn]
  "Creates an example GIT project with branches and tags set to test different scenarios"
  (println "FERD setup test!")
  (println (str "Compile path=" *compile-path*))
  (println (str "maven-target-dir=" maven-target-dir))
  (println (str "maven-script-source-dir=" maven-script-source-dir))
  (println (str "maven-bash-source-dir=" maven-bash-source-dir))
  (let [script (sh (str maven-bash-source-dir "/create-sample-git-project.sh") git-project-dir)]
    (println (script :out))
    (if-not (= 0 (script :exit)) (println (script :err)))
    (is (= 0 (script :exit))))
  (run-tests-fn))

(use-fixtures :once sample-git-project-fixture)

(defn get-commit-hash-by-description [descr]
  (let [git-cmd (str "cd " git-project-dir  "; git log --format=format:%H --grep='" descr "'")
        git (sh "bash" "-c" git-cmd)
        commit-hash (:out git)]
    (is (not (empty? commit-hash)))
    commit-hash))

(defn git-checkout [hash]
  (sh "bash" "-c" (str "cd " git-project-dir "; git checkout " hash)))

(defn infer-project-version []
  (:out (sh "bash" "-c" (str "cd " git-project-dir "; git describe"))))

(deftest test-infer-project-version-A
  ;; checkout on a particular commit and validate inferred version is the expected one
  (let [commit-hash (get-commit-hash-by-description "dev commit 5")]
    (git-checkout commit-hash)
    (println (str "infered project version=" (infer-project-version)))))

