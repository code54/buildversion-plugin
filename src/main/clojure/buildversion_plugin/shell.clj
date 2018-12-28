(ns buildversion-plugin.shell
  "A simple but flexible library for shelling out from Clojure.
  This code was copied from https://github.com/Raynes/conch"
  (:require [clojure.java.io :as io])
  (:import (java.util.concurrent TimeUnit TimeoutException)))

(defn proc
  "Spin off another process. Returns the process's input stream,
  output stream, and err stream as a map of :in, :out, and :err keys
  If passed the optional :dir and/or :env keyword options, the dir
  and enviroment will be set to what you specify. If you pass
  :verbose and it is true, commands will be printed. If it is set to
  :very, environment variables passed, dir, and the command will be
  printed. If passed the :clear-env keyword option, then the process
  will not inherit its environment from its parent process."
  [& args]
  (let [[cmd args] (split-with (complement keyword?) args)
        args (apply hash-map args)
        builder (ProcessBuilder. (into-array String cmd))
        env (.environment builder)]
    (when (:clear-env args)
      (.clear env))
    (doseq [[k v] (:env args)]
      (.put env k v))
    (when-let [dir (:dir args)]
      (.directory builder (io/file dir)))
    (when (:verbose args) (apply println cmd))
    (when (= :very (:verbose args))
      (when-let [env (:env args)] (prn env))
      (when-let [dir (:dir args)] (prn dir)))
    (when (:redirect-err args)
      (.redirectErrorStream builder true))
    (let [process (.start builder)]
      {:out (.getInputStream process)
       :in  (.getOutputStream process)
       :err (.getErrorStream process)
       :process process})))

(defn destroy
  "Destroy a process."
  [process]
  (.destroy (:process process)))

;; .waitFor returns the exit code. This makes this function useful for
;; both getting an exit code and stopping the thread until a process
;; terminates.
(defn exit-code
  "Waits for the process to terminate (blocking the thread) and returns
   the exit code. If timeout is passed, it is assumed to be milliseconds
   to wait for the process to exit. If it does not exit in time, it is
   killed (with or without fire)."
  ([process] (.waitFor (:process process)))
  ([process timeout]
     (try
       (.get (future (.waitFor (:process process))) timeout TimeUnit/MILLISECONDS)
       (catch Exception e
         (if (or (instance? TimeoutException e)
                 (instance? TimeoutException (.getCause e)))
           (do (destroy process)
               :timeout)
           (throw e))))))

(defn stream-to
  "Stream :out or :err from a process to an ouput stream.
  Options passed are fed to clojure.java.io/copy. They are :encoding to
  set the encoding and :buffer-size to set the size of the buffer.
  :encoding defaults to UTF-8 and :buffer-size to 1024."
  [process from to & args]
  (apply io/copy (process from) to args))

(defn stream-to-string
  "Streams the output of the process to a string and returns it."
  [process from & args]
  (with-open [writer (java.io.StringWriter.)]
    (apply stream-to process from writer args)
    (str writer)))
