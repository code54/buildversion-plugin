(ns inferProjectVersionPlugin.test
  (:use [inferProjectVersionPlugin.core :only [run-git infer-project-version] :as core]
        clojure.test
        clojure.java.shell
        [clojure.string :only [trim-newline blank?] ] ))
;  (:require [clojure.string :as string)


;; utility vars and funs
(def sample-project-dir (str maven-target-dir "/sample_project"))

(defn get-commit-hash-by-description [descr]
  (let [git (core/run-git sample-project-dir (str "log --all --format=format:%H --grep='^" descr "$'"))
        commit-hash (:out git)]
    (is (not (blank? commit-hash)))
    commit-hash))


;; fixtures
(defn sample-git-project-fixture [run-tests-fn]
  "Creates an example GIT project with branches and tags set to test different scenarios"
  (println "Building example GIT project for testing...")
  (let [script (sh (str maven-bash-source-dir "/create-sample-git-project.sh") sample-project-dir)
        exit-OK (zero? (script :exit)) ]
    (println (script :out))
    (if-not exit-OK
      (println (script :err)))
    (is exit-OK)

    (run-tests-fn)))

;(use-fixtures :once sample-git-project-fixture)

;; tests
;; (defn infer-project-version []
;;   ;; Call method under test here:
;;   (trim-newline (:out (run-git sample-project-dir "describe"))))


(deftest test-run-git
  (is (re-seq #"git version [\d\.]+"
                   (:out (core/run-git sample-project-dir "--version")))))


(defn- expect-version-for-commit [commit-descr expected-pattern]
  "Checkout on a particular commit and validate inferred version is the expected one"
  (let [commit-hash (get-commit-hash-by-description commit-descr)
        checkout (core/run-git sample-project-dir (str "checkout " commit-hash))
        checkout-OK (zero? (checkout :exit))
        actual-version (core/infer-project-version sample-project-dir)]

    (is checkout-OK (str "Checkout failure " (checkout :err) ))
    (is (re-seq expected-pattern actual-version)
        (str "Expecting version matching " expected-pattern
             " for commit with description: " commit-descr
             ". Actual: " actual-version ))))
  
(deftest test-infer-project-versions
  (expect-version-for-commit "dev commit 5" #"^1.1.0-SNAPSHOT.*")
  (expect-version-for-commit "dev commit 1" #"^1.0.0-SNAPSHOT.*"))

