(ns buildversion-plugin.mojo
  "Buildversion Maven Plugin"
  (:use clojure.maven.mojo.defmojo)
  (:require [clojure.maven.mojo.log :as log]
            [buildversion-plugin.git :as git]))


(defn- eval-custom-script [properties snippet-str]
  (let [props-as-bindings (vec (mapcat
                                (fn [[k v]] [(symbol (name k)) v])
                                properties))
        snippet (read-string snippet-str)]

    (eval `(let ~props-as-bindings ~snippet))))


(defmojo BuildVersionMojo
  
  {:goal "set-properties" :phase "initialize" }

  ;; Mojo parameters
  
  [project  {:expression "${project}" :required true :readonly true}

   tstamp-format {:alias "tstampFormat" :default "yyyyMMddHHmmss"
                  :typename "java.lang.String"}

   custom-script {:alias "customProperties" :typename "java.lang.String"} ]


  ;; Goal execution
  (let [git-versions (git/infer-project-version "." {:tstamp-format tstamp-format} )
        custom-versions (if custom-script
                          (eval-custom-script git-versions custom-script)
                          {})
        versions-map (merge git-versions custom-versions)
        props (.getProperties project)
        ]

    (log/debug (str "buildversion-plugin - Setting properties: "))
    (doseq [[prop value] versions-map]
      (log/debug (str (name prop) ": " value))
      (.put props (name prop) value))))




    ;; injecting project version does not working well :-(
    ;; (.setVersion project (:maven-artifact-version versions-map))
