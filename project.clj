(defproject buildversion-maven-plugin "1.0.0"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 ;[org.cloudhoist/clojure-maven-mojo-annotations "0.3.2-SNAPSHOT"]
                 [org.cloudhoist/clojure-maven-mojo-annotations "0.3.1"]
                 ;[org.cloudhoist/clojure-maven-mojo "0.3.2-SNAPSHOT"]
                 [org.cloudhoist/clojure-maven-mojo "0.3.1"]
                 [org.apache.maven/maven-plugin-api "3.0.3"]
                 [org.clojure/tools.trace "0.7.1"]
                 [conch "0.2.1"]]
  :dev-dependencies [[lein-swank "1.4.4"]
                     [lein-pprint "1.1.1"]
                     [radagast "1.1.0"]]

  :source-path "src/main/clojure"
  :compile-path "target/classes"
  :library-path "target/dependency"
  :test-path  "src/test/clojure"
  :resources-path "src/main/resource"
  :target-dir "target"
  )
