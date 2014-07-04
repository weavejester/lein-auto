(ns leiningen.auto
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [leiningen.core.main :as main])
  (:import (clojure.lang ExceptionInfo)
           (java.io File)))

(defn project-files [project]
  (file-seq (io/file (:root project))))

(defn modified-since [^File file timestamp]
  (> (.lastModified file) timestamp))

(defn modified-files [project timestamp]
  (->> (project-files project)
       (remove #(.isDirectory ^File %))
       (filter #(modified-since % timestamp))))

(defn grep [re coll]
  (filter #(re-find re (str %)) coll))

(def default-file-pattern #"\.(clj|cljs|cljx)$")

(defn run-task [project task args]
  (println "Running: lein" task (str/join " " args))
  (binding [main/*exit-process?* false]
    (try
      (main/resolve-and-apply project (cons task args))
      (catch ExceptionInfo _)))
  (println "Done.")
  (println "---"))

(defn add-ending-separator [^String path]
  (if (.endsWith path File/separator)
    path
    (str path File/separator)))

(defn remove-prefix [^String s ^String prefix]
  (if (.startsWith s prefix)
    (subs s (.length prefix))
    s))

(defn show-modified [project files]
  (let [root  (add-ending-separator (:root project))
        paths (map #(remove-prefix (str %) root) files)]
    (println "Files changed:" (str/join ", " paths))))

(defn auto
  "Executes the given task every time a file in the project is modified."
  [project task & args]
  (loop [time 0]
    (Thread/sleep 100)
    (if-let [files (->> (modified-files project time)
                        (grep default-file-pattern)
                        (seq))]
      (do (show-modified project files)
          (run-task project task args)
          (recur (System/currentTimeMillis)))
      (recur time))))
