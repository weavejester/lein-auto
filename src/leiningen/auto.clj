(ns leiningen.auto
  (:require [clojure.java.io :as io])
  (:import (java.io File)))

(defn project-files [project]
  (file-seq (io/file (:root project))))

(defn modified-since [^File file timestamp]
  (> (.lastModified file) timestamp))

(defn modified-files [project timestamp]
  (->> (project-files project)
       (remove #(.isDirectory ^File %))
       (filter #(modified-since % timestamp))))

(defn auto [project & args]
  (loop [time 0]
    (Thread/sleep 100)
    (if-let [files (seq (modified-files project time))]
      (do (prn files)
          (recur (System/currentTimeMillis)))
      (recur time))))
