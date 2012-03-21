(defproject buildversion-maven-plugin "1.0.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.cloudhoist/clojure-maven-mojo-annotations "0.3.2-SNAPSHOT"]
                 [org.cloudhoist/clojure-maven-mojo "0.3.2-SNAPSHOT"]
                 [org.apache.maven/maven-plugin-api "3.0.3"]
                 [org.clojure/tools.trace "0.7.1"]
                 [conch "0.2.1"]]

  :source-path "src/main/clojure"
  :compile-path "target/classes"
  :library-path "target/dependency"
  :test-path  "src/test/clojure"
  :resources-path "src/main/resource"
  :target-dir "target"
  )
