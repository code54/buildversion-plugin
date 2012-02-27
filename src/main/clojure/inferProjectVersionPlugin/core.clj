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
        [inferProjectVersionPlugin.git :as git]
        [clojure.string :only [trim-newline blank?] ]
        ;[clojure.tools.trace :only [dotrace deftrace]]
        ))

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

