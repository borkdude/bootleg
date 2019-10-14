(ns bootleg.core
  (:require [bootleg.transform :as transform]
            [bootleg.file :as file]
            [markdown.core :as markdown]
            [yaml.core :as yaml]
            [cljstache.core :as moustache]
            [net.cgrand.enlive-html :as enlive]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io]
            [clojure.walk :as walk]
            [clojure.string :as string]
            )
  (:gen-class))

(def version "0.1")

(def cli-options
  [
   ["-h" "--help" "Print the command line help"]
   ["-v" "--version" "Print the version string and exit"]])

(defn usage [options-summary]
  (->> ["Static site generator"
        ""
        "Usage: bootleg [options] yaml-file"
        ""
        "Options:"
        options-summary]
       (string/join \newline)))

(defn load-template [path file]
  (cond
    (string/ends-with? file ".md")
    (let [body (java.io.ByteArrayOutputStream.)]
      (markdown/md-to-html (io/input-stream (file/path-join path file)) body)
      (-> (.toString body)
          java.io.StringReader.
          enlive/html-resource
          first :content first :content))

    :else
    (-> (slurp (file/path-join path file))
        java.io.StringReader.
        enlive/html-resource)))

(defn load-and-process [{:keys [path load process vars] :as context}]
  (let [body-html
        (load-template path load)]
    (->
     (reduce
      (fn [acc {:keys [selector] :as step}]
        (let [selector-vec (into [] (map keyword (string/split selector #"\s+")))]
          (enlive/transform acc selector-vec (transform/make-transform context step))))
      (enlive/as-nodes body-html)
      process)

     enlive/emit*
     (->> (apply str))
     (moustache/render vars))))

(defn process-yaml-result [data path]
  (walk/postwalk
   #(if (:load %)
      (load-and-process (assoc % :path path))
      %)
   data))

(defn process [filename]
  (let [[path _] (file/path-split filename)]
    (-> filename
        yaml/from-file
        (process-yaml-result path))))

(defn -main
  "main entry point for site generation"
  [& args]
  (let [{:keys [options summary arguments]} (parse-opts args cli-options)]
    (cond
      (:help options)
      (println summary)

      (:version options)
      (println "Version:" version)

      (= 1 (count arguments))
      (-> arguments first process println)

      :else
      (do
        (println (usage summary))
        (System/exit 0)))))
