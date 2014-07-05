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

(def ansi-codes
  {:reset   "\u001b[0m"
   :black   "\u001b[30m" :gray           "\u001b[1m\u001b[30m"
   :red     "\u001b[31m" :bright-red     "\u001b[1m\u001b[31m"
   :green   "\u001b[32m" :bright-green   "\u001b[1m\u001b[32m"
   :yellow  "\u001b[33m" :bright-yellow  "\u001b[1m\u001b[33m"
   :blue    "\u001b[34m" :bright-blue    "\u001b[1m\u001b[34m"
   :magenta "\u001b[35m" :bright-magenta "\u001b[1m\u001b[35m"
   :cyan    "\u001b[36m" :bright-cyan    "\u001b[1m\u001b[36m"
   :white   "\u001b[37m" :bright-white   "\u001b[1m\u001b[37m"
   :default "\u001b[39m"})

(defn ansi [code & [default]]
  (str (ansi-codes code (ansi-codes default))))

(defn log [project & strs]
  (let [color (get-in project [:auto :color] true)
        text  (str/join " " strs)]
    (if color
      (println (str (ansi color :magenta) "auto> " text (ansi :reset)))
      (println (str "auto> " text)))))

(defn run-task [project task args]
  (log project "Running: lein" task (str/join " " args))
  (binding [main/*exit-process?* false]
    (try
      (main/resolve-and-apply project (cons task args))
      (catch ExceptionInfo _)))
  (log project "Done."))

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
    (log project "Files changed:" (str/join ", " paths))))

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
