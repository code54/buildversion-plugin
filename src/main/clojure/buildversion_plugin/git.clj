(ns buildversion-plugin.git
  "GIT implementation to infer current project version"
  (:import java.util.Date java.text.SimpleDateFormat)
  (:use    ;; [clojure.tools.trace :only [dotrace deftrace]]
   [clojure.java.io :only [reader]]
   [clojure.string :only [trim-newline replace-first split]] )
  (:require
   [conch.core :as sh] ))

(defn run-git
  ([args] (run-git "." args))
  ([project-dir args] 
     (sh/proc "bash" "-c" (str "cd " project-dir "; git " args))))

(defn run-git-wait
  ([args] (run-git-wait "." args))
  ([project-dir args]
     (sh/stream-to-string (run-git project-dir args) :out)))

(defn git-describe-first-parent [dir]
  "Return map with :git-tag (most recent tag on current branch (always following \"first-parent\" on merges))
and :git-tag-delta (number of commits -couting on first-parent paths only- from :git-tag to HEAD) "
  (let [p (run-git dir "log --oneline --decorate=short --first-parent")]

    (loop [x 0, lines (line-seq (reader (:out p))) ]
      (let [[_ hash tag] (re-find #"^(\w+) .*tag: ([^\)\,]+).*" (first lines))]
        (if (and (not tag) (next lines))
          (recur (inc x) (next lines))
          (do
            (sh/destroy p)
            {:git-tag tag, :git-tag-delta x}))))))

(defn infer-project-version [dir {tstamp-format "tstamp-format"}]
  "Infer the current project version from tags on the source-control system"

  (let [commit-tstamp (-> (run-git-wait dir "log -n 1 --format='%ct'")
                          trim-newline
                          (Long/parseLong)
                          (* 1000)
                          Date.)
        format-str (or tstamp-format "yyyyMMddHHmmss")
        commit-tstamp-str (.format (SimpleDateFormat. format-str)
                                   commit-tstamp)

        [short-hash long-hash] (split
                                (run-git-wait dir "log -n 1 --format='%h %H'")
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
            git-described  (run-git-wait dir (str "describe --tags --long --match " git-tag))
            ]

        (merge versioning-properties
               {:maven-artifact-version maven-artifact-version
                :descriptive-version (str (replace-first git-tag #"^v" "") "-" git-tag-delta)
                :packaging-version (str git-tag-delta) })))))





(comment

  (defn find-latest-tag-on-branch [dir]
    "Obtain most recent tag on current branch (always following \"first-parent\" on merges)"
    (let [log-line
          (:out (run-git dir "log --oneline --decorate=short --first-parent | grep 'tag: v' | head -n1"))]

      (second (re-find #".*tag: (v\d+\.\d+\.\d+[-_\d\w]*)[\)\,].*" log-line))))

  (defn- expect-tag-given-logline [log-line, tag]
    (with-redefs [git/run-git-wait (fn [ _ _] log-line )] ; "mock" call to git log
      (is (= (git/find-latest-tag-on-branch ".") tag))))

  (deftest test-find-latest-tag-on-branch
    ;;   v1.2.0-SNAPSHOT-8-ge34733d
    ;;   v1.2.0-SNAPSHOT-0-xxxxxxxx
    ;;   v1.2.0-RC-SNAPSHOT-0-xxxxxx
    ;;   v1.2.0-RC-SNAPSHOT-5-a3b4c533
    ;;   v1.2.0-3-a3b4c533
    ;;   v1.2.0-0-xxxxxxxx

    (expect-tag-given-logline "aa44944 (HEAD, tag: v9.9.9, origin/master, master) ..." "v9.9.9")
    (expect-tag-given-logline "c3bc9ff (tag: v1.11.0) TMS: Add..."                     "v1.11.0")
    (expect-tag-given-logline "c3bc9fx (tag: v1.10.0-dev) Blah blah..."                "v1.10.0-dev")))