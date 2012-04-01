(ns clojure.maven.mojo.defmojo
  (:require
   [clojure.string :as str]
   [clojure.maven.mojo.log :as log])
  (:import
   [org.apache.maven.plugin ContextEnabled Mojo MojoExecutionException]))

(defn assert-arg
  [b ^String msg]
  (if-not b
    (throw (IllegalArgumentException. msg))
    b))

(defn validate-param [[param opts]]
  (assert-arg
   (and (symbol? param) (map? opts))
   "Each Mojo parameter must be a symbol followed by a map of options")
  [param opts])

(defn key->annotation
  "Convert a keyword name (e.g. :requires-dependency) to the corresponding
camel-case symbol (e.g. RequiresDependency) and validate it's a Java
annotation."
  [k]
  (let [name (str "clojure.maven.annotations."
                  (-> (name k)
                      str/capitalize
                      (str/replace
                       #"-([a-zA-Z])" #(str/upper-case (second %)))))
        fail-msg (str "Cannot find corresponding Mojo annotation for " k)]
    (try (assert-arg (.isAnnotation (Class/forName name)) fail-msg)
         (catch ClassNotFoundException ex
           (throw (IllegalArgumentException. fail-msg))))
    (symbol name)))


(defn validate-body [body]
  (assert-arg (seq body) "Body of your Mojo definition is missing.")
  (assert-arg (= (count body) 1)
              (str "Body of your Mojo definition must be a single sexp. "
                   "You may use form (do ...) to enclose multiple sexps."))
  body)


(defmacro defmojo
  "Define a Mojo with the given name. This defines some common fields and
leaves you to just specify a body for the execute function.
Example:

    (defmojo MyClojureMojo

     {:goal \"simple\"
      :requires-dependency-resolution \"test\"
      :phase \"validate\" }

      ;; Mojo parameters
     [base-dir   {:expression \"${basedir}\" :required true :readonly true}
      project    {:expression \"${project}\" :required true :readonly true}
      output-dir {:defaultValue \"${project.build.outputDirectory}\"
                  :required true}]

     (do
       (println \"Hello Maven World!\")
       (println \"This is project \" (.getName project))))"
  [mojoType annotations-map parameters & body]
  (assert-arg (map? annotations-map)
              "First arg must be a map of Mojo annotations")
  (assert-arg (vector? parameters)
              (str  "Second arg must be a vector of parameters "
                    "(a name and options map for each param"))
  (let [mojo-annotations (into {} (map (fn [[k v]] [(key->annotation k) v])
                                       annotations-map))
        params (map validate-param (partition-all 2 parameters))
        body (validate-body body)]
    `(do
       (deftype
           ;; Mojo annotations
           ~(vary-meta mojoType merge mojo-annotations)

           ;; Mojo parameters
           ~(vec
             (concat
              (map (fn [[param options]]
                     (vary-meta param merge
                                {'clojure.maven.annotations.Parameter options}))
                   params)
              ;; pre-defined parameters
              `(~(with-meta 'log {:volatile-mutable true})
                ~'plugin-context)))

         ;; Mojo predefined methods
         Mojo
         ~'(setLog [_ logger] (set! log logger))
         ~'(getLog [_] log)

         ;; Mojo's suplied methods
         (~'execute [~'this]
           (log/with-log ~'log
             ~@body))

         ;; Plugin-Context handling
         ContextEnabled
         ~'(setPluginContext [_ context] (reset! plugin-context context))
         ~'(getPluginContext [_] @plugin-context))


       (defn ~(symbol (str "make-" mojoType))
         "Function to provide a no argument constructor"
         []
         (~(symbol (str mojoType "."))
          ~@(repeat (count params) nil) nil (atom nil))))))
