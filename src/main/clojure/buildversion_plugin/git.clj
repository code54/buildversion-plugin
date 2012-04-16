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

(defn git-describe-log-lines [log-lines-seq]
  "Given a seq of \"git log\" output lines, return map with :git-tag (most recent tag)
   and :git-tag-delta (number of commits to reach it)"
  (loop [i 0, lines log-lines-seq]
    (let [[_ hash tag] (re-find #"^(\w+) .*tag: (v[^\)\,]+).*" (first lines))]
      (if (and (not tag) (next lines))
        (recur (inc i) (next lines))
        [tag i]))))


(defn git-describe-first-parent [dir]
  "Return map with :git-tag (most recent tag on current branch (always following \"first-parent\" on merges))
and :git-tag-delta (number of commits -couting on first-parent paths only- from :git-tag to HEAD) "

  (let [p (run-git dir "log --oneline --decorate=short --first-parent")
        lines (line-seq (reader (:out p)))
        [tag delta] (git-describe-log-lines lines)]
    (sh/destroy p)
    {:git-tag tag, :git-tag-delta delta}))

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

        versioning-properties {:build-tag "N/A"
                               :build-version "N/A"
                               :build-tag-delta "0"
                               :build-tstamp commit-tstamp-str
                               :build-commit long-hash
                               :build-commit-abbrev short-hash }
                                                                                                                                                                     
        {:keys [git-tag git-tag-delta] } (git-describe-first-parent dir)]

    (if (nil? git-tag)
      versioning-properties
      
      (let [maven-artifact-version ((re-find #"v(.*)" git-tag) 1)]
        (merge versioning-properties
               {:build-tag maven-artifact-version
                :build-version (str (replace-first git-tag #"^v" "") "-" git-tag-delta "-" short-hash)
                :build-tag-delta (str git-tag-delta) })))))
