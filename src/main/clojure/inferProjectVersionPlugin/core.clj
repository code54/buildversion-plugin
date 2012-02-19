(ns inferProjectVersionPlugin.core
  "Simple Mojo in Clojure"
  (:import
   java.io.File
   [clojure.maven.annotations
    Goal RequiresDependencyResolution Parameter Component]
   org.apache.maven.plugin.ContextEnabled
   org.apache.maven.plugin.Mojo
   org.apache.maven.plugin.MojoExecutionException)
  (:use clojure.java.shell
        [clojure.string :only [trim-newline blank?] ] ))
        [clojure.tools.trace :only [dotrace deftrace]] ))

(defn run-git
  ([args] (run-git "." args))
  ([project-dir args] 
     (sh "bash" "-c" (str "cd " project-dir "; git " args))))

(defn infer-project-version [project-dir]
  "Infer the current project version from information from the source-control system"

  ;; first obtain most recent tag on current branch (always following "first-parent" on merges)
  ;; Then we parse the version from the tag. Sample output we are parsing here:
  ;; aa44944 (HEAD, tag: v9.9.9, origin/master, origin/HEAD, master) Changes...
  ;; c3bc9ff (tag: v1.11.0) TMS: Add...
  ;; c3bc9fx (tag: v1.10.0-dev) Blah blah...

  (let [most-recent-tag (:out (run-git project-dir "log --oneline --decorate=short --first-parent | grep 'tag: v' | head -n1"))
        matched-version (second (re-find #".*tag: (v\d+\.\d+\.\d+[-_\d\w]*)[\)\,].*"
                                         most-recent-tag))
    
        ;; now call git-describe forcing it to match the tag we just found
        git-described ((run-git project-dir (str "describe --tags --long --match " matched-version)) :out) ]

    ;; remove leading "v"
    (.substring (trim-newline git-described) 1)))

(deftype
    ^{Goal "simple"
      RequiresDependencyResolution "test"}
    MyClojureMojo
  [
   ^{Parameter
     {:expression "${basedir}" :required true :readonly true}}
   base-directory

   ^{Parameter
     {:expression "${project}" :required true :readonly true}}
   project

   ;; ^{Parameter
   ;;   {:defaultValue "${project.compileClasspathElements}"
   ;;    :required true :readonly true :description "Compile classpath"}}
   ;; classpath-elements

   ;; ^{Parameter
   ;;   {:defaultValue "${project.testClasspathElements}"
   ;;    :required true :readonly true}}
   ;; test-classpath-elements

   ^{Parameter
     {:defaultValue "${project.build.outputDirectory}" :required true}}
   output-directory

   ^{:volatile-mutable true}
   log

   plugin-context
   ]

  Mojo
  (execute [_]
    (.info log (str "* Infering project version *" output-directory))
    (.info log (str "* project.version = " (.getVersion project)))
    )

  (setLog [_ logger] (set! log logger))
  (getLog [_] log)

  ContextEnabled
  (setPluginContext [_ context] (reset! plugin-context context))
  (getPluginContext [_] @plugin-context))

(defn make-MyClojureMojo
  "Function to provide a no argument constructor"
  []
  (MyClojureMojo. nil nil nil nil (atom nil)))

