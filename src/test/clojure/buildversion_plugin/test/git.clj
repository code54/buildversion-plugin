(ns buildversion-plugin.test.git
  (:use [buildversion-plugin.git :only [run-git infer-project-version] :as git]
        clojure.test
        clojure.java.shell
        [clojure.java.io :only [file]]
        clojure.tools.trace
        [clojure.string :only [trim-newline blank?] ] ))

;; These vars are def'd in the pom.xml when tests are run by mvn's zi plugin.
;; I def some default values here for interactive development:
(def maven-target-dir "/tmp")
(def maven-bash-source-dir (.getCanonicalPath (java.io.File.
                                               "./src/test/bash")))
;; (println (str "*** maven-target-dir: " maven-target-dir))
;; (println (str "*** maven-bash-source-dir: " maven-bash-source-dir))


;;
;; fixtures
;;
(def sample-project-dir (str maven-target-dir "/sample_project"))
(defn sample-git-project-fixture [run-tests-fn]
  "Creates an example GIT project with branches and tags set to test different scenarios"

  (if (.isDirectory (file (str sample-project-dir "/.git")))
    (println "Example GIT project dir found... re-using it")
    
    (let [script (sh (str maven-bash-source-dir "/create-sample-git-project.sh") sample-project-dir)
          exit-OK (zero? (script :exit)) ]
      
      (println "Building example GIT project for testing...")
      (println (script :out))

      (if-not exit-OK (println (script :err)))
      (is exit-OK)))

  
  (run-tests-fn))

(use-fixtures :once sample-git-project-fixture)


;;
;; Tests
;;
(deftest test-run-git
  (is (re-seq #"git version [\d\.]+"
              (:out (git/run-git sample-project-dir "--version")))))


(defn- expect-tag-given-logline [log-line, tag]
  (with-redefs [git/run-git (fn [ _ _] {:out log-line} )] ; "mock" call to git log
     (is (= (git/find-latest-tag-on-branch ".") tag))))

(deftest test-find-latest-tag-on-branch

  (expect-tag-given-logline "aa44944 (HEAD, tag: v9.9.9, origin/master, master) ..." "v9.9.9")
  (expect-tag-given-logline "c3bc9ff (tag: v1.11.0) TMS: Add..."                     "v1.11.0")
  (expect-tag-given-logline "c3bc9fx (tag: v1.10.0-dev) Blah blah..."                "v1.10.0-dev"))



(defn get-commit-hash-by-description [descr]
  "Given a git commit description, it returns the hash of a commit matching that description"
  (let [git (git/run-git sample-project-dir (str "log --all --format=format:%H --grep='^" descr "$'"))
        commit-hash (:out git)]
    (is (not (blank? commit-hash)) (str "Couldn't find hash for commit descr" descr))
    commit-hash))

(defn assert-for-commit [commit-descr expected-patterns]
  "Checkout on a particular commit and validate inferred versions match the given regexp patterns"

  (let [commit-hash (get-commit-hash-by-description commit-descr)
        checkout (git/run-git sample-project-dir (str "checkout " commit-hash))
        checkout-OK (zero? (checkout :exit))
        actual-versions (git/infer-project-version sample-project-dir)]

    (is checkout-OK (str "Checkout failure " (checkout :err) ))

    (doall (map (fn [key] (is (re-find (expected-patterns key) (actual-versions key))
                              (str "Testing " key " for commit '" commit-descr "'")))
                (keys expected-patterns)))))


(deftest test-infer-project-versions

  (assert-for-commit "First tagged commit"
                     {:descriptive-version    #"^1.0.0-SNAPSHOT$"
                      :maven-artifact-version #"^1.0.0-SNAPSHOT$"
                      :packaging-version      #"0"
                      :tstamp-version         #"\d+"
                      :commit-version         #"[a-f\d]+"
                      })

  (assert-for-commit "dev commit 5"
                     {:descriptive-version      #"1.1.0-SNAPSHOT"
                      :maven-artifact-version   #"^1.1.0-SNAPSHOT$"
                      :packaging-version        #"7"
                      :tstamp-version           #"\d+"
                      :commit-version           #"[a-f\d]+"
                      })

  (assert-for-commit "dev commit 1"
                     {:descriptive-version      #"^1.0.0-SNAPSHOT-1.*"
                      :maven-artifact-version   #"^1.0.0-SNAPSHOT"
                      :packaging-version        #"1"
                      :tstamp-version           #"\d+"
                      :commit-version           #"[a-f\d]+"
                      })

  ;; No tag is reacheable from this commit:
  (assert-for-commit "Initial commit. Before any tag"
                     {:descriptive-version      #"N/A"
                      :maven-artifact-version   #"N/A"
                      :packaging-version        #"0"
                      :tstamp-version           #"\d+"
                      :commit-version           #"[a-f\d]+"
                      }))

