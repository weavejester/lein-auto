(ns leiningen.auto
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [leiningen.core.main :as main])
  (:import [clojure.lang ExceptionInfo]
           [java.nio.file FileSystems StandardWatchEventKinds]
           [com.sun.nio.file SensitivityWatchEventModifier]
           [java.io File]))

(defn directory-directories [dir]
  (->> (io/file dir) (file-seq) (filter (memfn isDirectory))))

(defn modified-since [^File file timestamp]
  (> (.lastModified file) timestamp))

(defn grep [re coll]
  (filter #(re-find re (str %)) coll))

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

(defn log [{:keys [log-color]} & strs]
  (let [text (str/join " " strs)]
    (if log-color
      (println (str (ansi-codes log-color) "auto> " text (ansi-codes :reset)))
      (println (str "auto> " text)))))

(defn run-task [project task args]
  (binding [main/*exit-process?* false]
    (main/resolve-and-apply project (cons task args))))

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
    (str/join ", " paths)))

(def default-config
  {:file-pattern #"\.(clj|cljs|cljx|cljc)$"
   :wait-time    50
   :log-color    :magenta})

(defn default-paths [project]
  (concat (:source-paths project)
          (:java-source-paths project)
          (:test-paths project)))

(defn auto
  "Executes the given task every time a file in the project is modified."
  [project task & args]
  (let [config (merge default-config
                      {:paths (default-paths project)}
                      (get-in project [:auto :default])
                      (get-in project [:auto task]))
        directories (->> (mapcat directory-directories (:paths config))
                          (seq))
        fs (FileSystems/getDefault)
        watcher (.newWatchService fs)
        keys (apply merge
              (for [dir directories]
                (let [p (.toPath dir)
                      key (.register p watcher
                        (into-array [StandardWatchEventKinds/ENTRY_CREATE StandardWatchEventKinds/ENTRY_MODIFY StandardWatchEventKinds/ENTRY_DELETE])
                        (into-array [SensitivityWatchEventModifier/HIGH]))]
                    {key dir})))]
      (log config "lein-auto now watching:" (:paths config))
      (loop [keys keys]
        (let [key (.take watcher)]
          (if (contains? keys key)
            (do
              (doseq [event (.pollEvents key) :let [fp (str (get keys key) "/" (-> event .context .toString))]]
                (log config "Changes to:" fp))
              (log config "Running: lein" task (str/join " " args))
              (try
                (run-task project task args)
                (log config "Completed.")
                (catch ExceptionInfo _
                  (log config "Failed.")))
              (if (.reset key)
                (recur keys)
                (recur (dissoc key key))))
            (recur keys))))))