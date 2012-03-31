(ns buildversion-plugin.test.git
  (:use [clojure.java.io :only [file]]
        [clojure.string :only [trim-newline blank?] ]
;        clojure.tools.trace
        clojure.test)
  (:require [conch.core :as sh]
            :reload [buildversion-plugin.git :as git]))

;; These system properties are set in the pom.xml by mvn's zi:test's "initScript".
;; Some default values here for interactive development
(def maven-target-dir (java.lang.System/getProperty
                       "buildversion.maven-target-dir" "/tmp"))
(def maven-bash-source-dir
  (.getCanonicalPath (java.io.File.
                      (java.lang.System/getProperty "buildversion.maven-bash-source-dir"
                                                    "./src/test/bash"))))
(println (str "buildversion tests. Using maven-target-dir: " maven-target-dir))
(println (str "buildversion tests. Using maven-bash-source-dir: " maven-bash-source-dir))


;;
;; fixtures
;;
(def sample-project-dir (str maven-target-dir "/sample_project"))
(defn sample-git-project-fixture [run-tests-fn]
  "Creates an example GIT project with branches and tags set to test different scenarios"

  (if (.isDirectory (file (str sample-project-dir "/.git")))
    (println "Example GIT project dir found... re-using it")
    
    (let [script (sh/proc (str maven-bash-source-dir "/create-sample-git-project.sh") sample-project-dir)
          exit-OK (zero? (sh/exit-code script)) ]
      
      (println "Building example GIT project for testing...")
      (println (script :out))

      (if-not exit-OK (println (script :err)))
      (is exit-OK)))

  
  (run-tests-fn))

(use-fixtures :once sample-git-project-fixture)

;;
;; Helper funs
;;
(defn- get-commit-hash-by-description [descr]
  "Given a git commit description, it returns the hash of a commit matching that description"
  (let [git (git/run-git-wait sample-project-dir (str "log --all --format=format:%H --grep='^" descr "$'"))
        commit-hash git]
    (is (not (blank? commit-hash)) (str "Couldn't find hash for commit descr" descr))
    commit-hash))

(defn- assert-for-commit [commit-descr expected-patterns]
  "Checkout on a particular commit and validate inferred versions match the given regexp patterns"

  (let [commit-hash (get-commit-hash-by-description commit-descr)
        checkout (git/run-git sample-project-dir (str "checkout " commit-hash))
        checkout-OK (zero? (sh/exit-code checkout))
        actual-versions (git/infer-project-version sample-project-dir {})]

    (is checkout-OK (str "Checkout failure " (checkout :err) ))

    (doall (map (fn [key] (is (re-find (expected-patterns key) (actual-versions key))
                              (str "Testing " key " for commit '" commit-descr "'")))
                (keys expected-patterns)))))


;;
;; Tests
;;
(deftest test-run-git
  (is (re-seq #"git version [\d\.]+"
              (git/run-git-wait sample-project-dir "--version"))))

(deftest test-infer-project-versions

  (assert-for-commit "First tagged commit"
                     {:build-version    #"^1.0.0-SNAPSHOT-0.*"
                      :build-tag #"^1.0.0-SNAPSHOT$"
                      :build-tag-delta      #"0"
                      :build-tstamp         #"\d+"
                      :build-commit         #"[a-f\d]+"
                      })

  (assert-for-commit "dev commit 5"
                     {:build-version      #"1.1.0-SNAPSHOT-3"
                      :build-tag   #"^1.1.0-SNAPSHOT$"
                      :build-tag-delta        #"3"
                      :build-tstamp           #"\d+"
                      :build-commit           #"[a-f\d]+"
                      })

  (assert-for-commit "dev commit 1"
                     {:build-version      #"^1.0.0-SNAPSHOT-1.*"
                      :build-tag   #"^1.0.0-SNAPSHOT"
                      :build-tag-delta        #"1"
                      :build-tstamp           #"\d+"
                      :build-commit           #"[a-f\d]+"
                      })

  ;; No tag is reacheable from this commit:
  (assert-for-commit "Initial commit. Before any tag"
                     {:build-version      #"N/A"
                      :build-tag   #"N/A"
                      :build-tag-delta        #"0"
                      :build-tstamp           #"\d+"
                      :build-commit           #"[a-f\d]+"
                      }))


(deftest test-git-describe-first-parent

  ;; test against actual (sample) GIT repo
  ;; (see sample-git-project-fixture above)
  (are [commitish expected-tag expected-delta]
       (do
         (git/run-git-wait sample-project-dir (str "checkout " commitish))
         (= (git/git-describe-first-parent sample-project-dir)
            {:git-tag expected-tag  :git-tag-delta expected-delta}))

       "develop" "v1.2.0-SNAPSHOT" 2
       "v1.2.0-SNAPSHOT" "v1.2.0-SNAPSHOT" 0
       "master" "v1.1.1" 0 )

  ;; test agains some fake "git log" output to cover more scenarios
  (are [lines tag commit-count]
       (= (git/git-describe-log-lines lines) [tag commit-count])

       ["aa44944 (HEAD, tag: v9.9.9, origin/master, master) ..."] "v9.9.9"      0
       ["c3bc9ff (tag: v1.11.0) TMS: Add..."                    ] "v1.11.0"     0
       ["c3bc9fx (tag: v1.10.0-dev) Blah blah..."               ] "v1.10.0-dev" 0 

       ["aabbccd Dummy commit"
        "bbccddee Dummy commmit2"
        "c3bc9fx (tag: v1.10.0-dev) Blah blah..."               ] "v1.10.0-dev" 2

       ;; no tags:
       ["aabbccd Dummy commit1"
        "bbccdde Dummy commit2"
        "ccddeef Dymmy commit3"] nil 2))


(deftest test-tstamp-format-option
  (let [actual-versions (git/infer-project-version sample-project-dir
                                                   { "tstamp-format" "SSS"})]
    (println actual-versions)
    (is (re-find #"000" (:build-tstamp actual-versions))
        "Always expecting 000 milliseconds. Git precision is up to seconds")))

