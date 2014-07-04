(ns leiningen.auto
  (:require [clojure.java.io :as io]
            [leiningen.core.main :as main])
  (:import (java.io File)))

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

(defn auto
  "Executes the given task every time a file in the project is modified."
  [project task & args]
  (loop [time 0]
    (Thread/sleep 100)
    (if-let [files (->> (modified-files project time)
                        (grep default-file-pattern)
                        (seq))]
      (do (main/resolve-and-apply project (cons task args))
          (recur (System/currentTimeMillis)))
      (recur time))))
