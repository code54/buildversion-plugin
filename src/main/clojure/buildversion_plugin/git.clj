(ns buildversion-plugin.git
  "GIT implementation to infer current project version"
  (:import java.util.Date java.text.SimpleDateFormat)
  (:use    ;; [clojure.tools.trace :only [dotrace deftrace]]
   [clojure.java.io :only [reader]]
   [clojure.string :only [trim-newline replace-first split join]] )
  (:require
   [conch.core :as sh] ))


(def ^:dynamic *debug-fn* #(println %))
(def ^:dynamic *git-cmd* "git")


(defn run-git
  ([args] (run-git "." args))
  ([project-dir args]
     (*debug-fn* (str "Running cmd: " *git-cmd* " " (join " " args)))
     (apply sh/proc `(~*git-cmd* ~@args :dir ~project-dir))))

(defn run-git-wait
  ([args] (run-git-wait "." args))
  ([project-dir args]
     (let [proc (run-git project-dir args)
           ok (zero? (sh/exit-code proc))
           stdout (sh/stream-to-string proc :out)
           stderr (sh/stream-to-string proc :err)]
       (if ok
         stdout
         (throw (RuntimeException.
                 (str "Execution of Git command failed: " stderr)))))))

(defn git-describe-log-lines [log-lines-seq]
  "Given a seq of \"git log\" output lines, return map with :git-tag (most recent
  tag) and :git-tag-delta (number of commits to reach it)"
  (loop [i 0, lines log-lines-seq]
    (let [line (first lines)
          [_ hash tag] (re-find #"^(\w+) .*tag: (v[^\)\,]+).*" line)]
      (*debug-fn* (str "Processing git-log line: " line))
      (if (and (not tag) (next lines))
        (recur (inc i) (next lines))
        [tag i]))))

(defn git-describe-first-parent [dir]
  "Return map with :git-tag (most recent tag on current branch (always following
  \"first-parent\" on merges)) and :git-tag-delta (number of commits -couting on
  first-parent paths only- from :git-tag to HEAD)"

  (let [p (run-git dir ["log" "--oneline" "--decorate=short" "--first-parent"])
        lines (line-seq (reader (:out p)))
        [tag delta] (git-describe-log-lines lines)]
    (sh/destroy p)
    {:git-tag tag, :git-tag-delta delta}))

(defn infer-project-version [dir {:keys [tstamp-format git-cmd debug-fn]
                                  :or {tstamp-format "yyyyMMddHHmmss"
                                             git-cmd "git"
                                            debug-fn #(println %)}}]
  "Infer the current project version from tags on the source-control system"

  (binding [*debug-fn* debug-fn, *git-cmd* git-cmd]
    (let [commit-tstamp (-> (run-git-wait dir ["log" "-n" "1" "--format=%ct"])
                            trim-newline
                            (Long/parseLong)
                            (* 1000)
                            Date.)
          format-str (or tstamp-format "yyyyMMddHHmmss")
          commit-tstamp-str (.format (SimpleDateFormat. format-str)
                                     commit-tstamp)

          [short-hash long-hash]  (split
                                   (run-git-wait dir
                                                 ["log" "-n" "1" "--format=%h %H"])
                                   #"\s+")

          versioning-properties {:build-tag "N/A"
                                 :build-version "N/A"
                                 :build-tag-delta "0"
                                 :build-tstamp commit-tstamp-str
                                 :build-commit long-hash
                                 :build-commit-abbrev short-hash }

          {:keys [git-tag git-tag-delta] } (git-describe-first-parent dir)]

      (if (nil? git-tag)
        versioning-properties

        (let [maven-artifact-version ((re-find #"v(.*)" git-tag) 1)
              version (replace-first git-tag #"^v" "")]
          (merge versioning-properties
                 {:build-tag maven-artifact-version
                  :build-version (if (== git-tag-delta 0)
                                   version
                                   (str version "-"
                                        git-tag-delta "-" short-hash))
                  :build-tag-delta (str git-tag-delta) }))))))
