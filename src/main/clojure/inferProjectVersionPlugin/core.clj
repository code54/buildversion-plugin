(ns inferProjectVersionPlugin.core
  "Simple Mojo in Clojure"

  (:use clojure.java.shell
        clojure.maven.mojo.defmojo
        [clojure.string :only [trim-newline blank?] ]
        ;[clojure.tools.trace :only [dotrace deftrace]]
        )
  (:require [clojure.maven.mojo.log :as log]
            [inferProjectVersionPlugin.git :as git]))


(defmojo MyClojureMojo
  
  {:goal "simple"
   :requires-dependency-resolution "test"
   :phase "validate" }

  ;; Mojo parameters
  [
   base-dir   {:expression "${basedir}" :required true :readonly true}
   project          {:expression "${project}" :required true :readonly true}
   output-dir {:defaultValue "${project.build.outputDirectory}" :required true}]

  (do 
    (log/info (str "project class: " (class project)))
    (log/info (str "project: "  project))
    (log/info (str "outputDirectory: "  output-dir))
    (let [versions-map (git/infer-project-version ".")
          props (.getProperties project)]
      
      (doall (map (fn [[ k v]] (.put props (name k) v)) versions-map))

                                        ; inject project version
      (.setVersion project (:maven-artifact-version versions-map))
      (log/info (str "* Java class for 'project': " (class project)))
      (log/info (str "* output-directory *" output-dir))
      (log/info (str "* project.version = " (.getVersion project)))
      )))
