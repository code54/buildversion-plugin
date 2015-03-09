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
  [project       {:expression "${project}"
                  :required true
                  :readonly true}
   base-dir      {:expression "${basedir}"
                  :required true
                  :readonly true}
   tstamp-format {:alias    "tstampFormat"
                  :default  "yyyyMMddHHmmss"
                  :typename "java.lang.String"}
   custom-script {:alias    "customProperties"
                  :typename "java.lang.String"}
   git-cmd       {:alias    "gitCmd"
                  :default  "git"
                  :typename "java.lang.String"}
   fail-on-error {:alias    "failOnError"
                  :default  true
                  :typename "java.lang.Boolean"}   ]

  ;; Goal execution
  (try
	  (let [log-fn #(.debug log/*plexus-log* (str "[buildversion-plugin] " %))
	
	        inferred-props (git/infer-project-version base-dir
	                                                {:tstamp-format tstamp-format
	                                                 :git-cmd (or git-cmd "git")
	                                                 :debug-fn log-fn} )
	        final-props (if custom-script
	                      (merge inferred-props
	                             (eval-custom-script inferred-props custom-script))
	                      inferred-props)
	        maven-project-props (.getProperties project)]
	
	    (log-fn "Setting properties: ")
	    (doseq [[prop value] final-props]
	      (log-fn (str (name prop) ": " value))
	      (.put maven-project-props (name prop) value)))
	      
	(catch RuntimeException e
		(if (false? fail-on-error)
			(if (.contains (.getMessage e) "Not a git repository")
				(println "!!! Not a Git repository : failOnError parameter false : exit plugin without error")
				(throw e))
	    	(throw e)))))
	
	
	    ;; injecting project version does not work well :-(
	    ; (if-let [ver (:build-tag final-props)]
	    ;   (.setVersion project))

