(ns buildversion-plugin.git
  "GIT implementation to infer current project version"
  (:import java.util.Date java.text.SimpleDateFormat)
  (:use
   [clojure.java.io :only [reader]]
   [clojure.string :only [trim-newline replace-first split]] )
  ;; [clojure.tools.trace :only [dotrace deftrace]]

  (:require
   [clojure.java.shell :as shell]
   [conch.core :as sh] ))

(defn run-git
  ([args] (run-git "." args))
  ([project-dir args] 
     (shell/sh "bash" "-c" (str "cd " project-dir "; git " args))))

(defn run-git2
  ([args] (run-git2 "." args))
  ([project-dir args] 
     (sh/proc "bash" "-c" (str "cd " project-dir "; git " args))))



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

    (second (re-find #".*tag: (v\d+\.\d+\.\d+[-_\d\w]*)[\)\,].*" log-line))))


(defn git-describe-first-parent [dir]
  "Return map with :git-tag (most recent tag on current branch (always following \"first-parent\" on merges))
and :git-tag-delta (number of commits -couting on first-parent paths only- from :git-tag to HEAD) "
  (let [p (run-git2 dir "log --oneline --decorate=short --first-parent")]

    (loop [x 0, lines (line-seq (reader (:out p))) ]
      (let [[_ hash tag] (re-find #"^(\w+) .*tag: ([^\)\,]+).*" (first lines))]
        (if (and (not tag) (next lines))
          (recur (inc x) (next lines))
          (do
            (sh/destroy p)
            {:git-tag tag, :git-tag-delta x}))))))


(defn infer-project-version [dir]
  "Infer the current project version from tags on the source-control system"

  (let [commit-tstamp (-> (:out (run-git dir "log -n 1 --format='%ct'"))
                             trim-newline
                             (Long/parseLong)
                             (* 1000)
                             Date.)

        commit-tstamp-str (. (SimpleDateFormat. "yyyyMMddHHmmss") format commit-tstamp)

        [short-hash long-hash] (split
                                (:out (run-git dir "log -n 1 --format='%h %H'"))
                                #" " )

        versioning-properties {:maven-artifact-version "N/A"
                               :descriptive-version "N/A"
                               :packaging-version "0"
                               :tstamp-version commit-tstamp-str
                               :commit-version long-hash
                               :short-commit-version short-hash }
                                                                                                                                                                     
        {:keys [git-tag git-tag-delta] } (git-describe-first-parent dir)]

    (if (nil? git-tag)
      versioning-properties
      
      (let [maven-artifact-version ((re-find #"v(.*)" git-tag) 1)
            git-described (:out (run-git dir (str "describe --tags --long --match " git-tag)))
            ]

        (merge versioning-properties
               {:maven-artifact-version maven-artifact-version
                :descriptive-version (str (replace-first git-tag #"^v" "") "-" git-tag-delta)
                :packaging-version (str git-tag-delta) })))))

