(ns buildversion-plugin.mojo
  "Buildversion Maven Plugin"
  (:use clojure.maven.mojo.defmojo)
  (:require [clojure.maven.mojo.log :as log]
            [buildversion-plugin.git :as git]))

(defmojo BuildVersionMojo
  
  {:goal "set-properties" :phase "initialize" }

  ;; Mojo parameters
  [project  {:expression "${project}" :required true :readonly true}
   tstamp-format {:alias "tstampFormat" :default "yyyyMMddHHmmss"
                  :typename "java.lang.String"} ]


  ;; Goal execution
  (let [versions-map (git/infer-project-version "." {"tstamp-format" tstamp-format})
        props (.getProperties project)]

    (log/debug (str "buildnumber-plugin - Setting properties: "))
    (doseq [[prop value] versions-map]
      (log/debug (str (name prop) "=" value))
      (.put props (name prop) value))))




    ;; injecting project version does not working well :-(
    ;; (.setVersion project (:maven-artifact-version versions-map))

