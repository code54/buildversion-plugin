(ns inferProjectVersionPlugin.core
  (:use clojure.test
        clojure.java.shell
        [clojure.string :only [trim-newline blank?] ] ))
;  (:require [clojure.string :as string)


;; utility vars and funs
(def git-project-dir (str maven-target-dir "/sample_project"))

(defn run-git [args]
  (sh "bash" "-c" (str "cd " git-project-dir "; git " args)))

(defn get-commit-hash-by-description [descr]
  (let [git (run-git (str "log --format=format:%H --grep='" descr "'"))
        commit-hash (:out git)]
    (is (not (blank? commit-hash)))
    commit-hash))

(defn infer-project-version []
  (trim-newline (:out (run-git "describe"))))

;; fixtures
(defn sample-git-project-fixture [run-tests-fn]
  "Creates an example GIT project with branches and tags set to test different scenarios"
  (println "Building example GIT project for testing...")
  (let [script (sh (str maven-bash-source-dir "/create-sample-git-project.sh") git-project-dir)
        exit-OK (= 0 (script :exit)) ]
    (println (script :out))
    (if-not exit-OK
      (println (script :err)))
    (is exit-OK)

    (run-tests-fn)))

;(use-fixtures :once sample-git-project-fixture)

;; tests
(defn expect-version-for-commit [commit-descr expected-version]
  "Checkout on a particular commit and validate inferred version is the expected one"
  (let [commit-hash (get-commit-hash-by-description commit-descr)]
    (run-git (str "checkout " commit-hash))
    (is (= (infer-project-version) expected-version) (str "Testing commit with description: " commit-descr))))
  
(deftest test-infer-project-versions
  (expect-version-for-commit "dev commit 5" "1.0.0")
  (expect-version-for-commit "dev commit 1" "1.0.0")
  )

