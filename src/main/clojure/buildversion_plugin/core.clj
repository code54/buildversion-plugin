(ns buildversion-plugin.core
  "Buildversion Maven Plugin"
  (:use clojure.maven.mojo.defmojo :only defmojo)
  (:require [clojure.maven.mojo.log :as log]
            [buildversion-plugin.git :as git]))

(defmojo BuildVersionMojo
  
  {:goal "simple"
   :requires-dependency-resolution "test"
   :phase "validate" }

  ;; Mojo parameters
  [project  {:expression "${project}" :required true :readonly true}]


  ;; Goal execution
  (let [versions-map (git/infer-project-version ".")
        props (.getProperties project)]

    (log/debug (str "buildnumber-plugin - Setting properties: "))
    (doseq [[prop value] versions-map]
      (log/debug (str (name prop) "=" value))
      (.put props (name prop) value))))

    ;; injecting project version does not working well :-(
    ;; (.setVersion project (:maven-artifact-version versions-map))

