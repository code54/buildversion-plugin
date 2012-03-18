(ns buildversion-plugin.git
  "GIT implementation to infer current project version"
  (:import java.util.Date java.text.SimpleDateFormat)
  (:use clojure.java.shell
        ; [clojure.tools.trace :only [dotrace deftrace]]
        [clojure.string :only [trim-newline replace-first]] ))

(defn run-git
  ([args] (run-git "." args))
  ([project-dir args] 
     (sh "bash" "-c" (str "cd " project-dir "; git " args))))



(defn commits-until-tag [dir]
  "Given the directory of a git repo, return a tuple with the most recent tag
and the number of commits from HEAD to reach that tag (always following
first-parents on merges). This is NOT the same as calling git-describe.
See:
  http://kerneltrap.org/mailarchive/git/2010/9/21/40071/thread
  http://www.xerxesb.com/2010/git-describe-and-the-tale-of-the-wrong-commits/"

  (let [log-line
        (:out (run-git dir "log --oneline --decorate=short --first-parent | grep 'tag: v' | head -n1"))]))


(defn find-latest-tag-on-branch [dir]
  "Obtain most recent tag on current branch (always following \"first-parent\" on merges)"
  (let [log-line
        (:out (run-git dir "log --oneline --decorate=short --first-parent | grep 'tag: v' | head -n1"))]

    ;; expected output to parse:
    ;;   v1.2.0-SNAPSHOT-8-ge34733d
    ;;   v1.2.0-SNAPSHOT-0-xxxxxxxx
    ;;   v1.2.0-RC-SNAPSHOT-0-xxxxxx
    ;;   v1.2.0-RC-SNAPSHOT-5-a3b4c533
    ;;   v1.2.0-3-a3b4c533
    ;;   v1.2.0-0-xxxxxxxx
    (second (re-find #".*tag: (v\d+\.\d+\.\d+[-_\d\w]*)[\)\,].*" log-line))))
    
(defn infer-project-version [dir]
  "Infer the current project version from tags on the source-control system"

  (let [commit-tstamp (-> (:out (run-git dir "log -n 1 --format='%ct'"))
                             trim-newline
                             (Long/parseLong)
                             (* 1000)
                             Date.)
        commit-tstamp-str (. (SimpleDateFormat. "yyyyMMddHHmmss") format commit-tstamp)
        versioning-properties {:maven-artifact-version "N/A"
                               :descriptive-version "N/A"
                               :packaging-version "0"
                               :tstamp-version commit-tstamp-str
                               :commit-version "hash-here"}
        
        ;; call git-describe forcing it to match the latest tag we found
        git-tag (find-latest-tag-on-branch dir)]

    (if (nil? git-tag)
      versioning-properties
      
      (let [git-described (:out (run-git dir (str "describe --tags --match " git-tag)))
            git-described-long ((run-git dir (str "describe --tags --long --match " git-tag)) :out)
            maven-artifact-version ((re-find #"v(.*)" git-tag) 1)
            after-tag (replace-first git-described-long git-tag "")
            [_, commits-ahead, commit-hash] (re-find #"-(\d+)-([\d\w]+)" after-tag)]

        (merge versioning-properties
               {:maven-artifact-version maven-artifact-version
                :descriptive-version (replace-first git-described #"^v" "")
                :packaging-version commits-ahead })))))

