(ns test-bootleg-pod
  (:require [babashka.pods :as pods]))

(pods/load-pod ["lein" "run"])
#_ (pods/load-pod ["bootleg"])

(require '[pod.retrogradeorbit.bootleg.markdown :as markdown]
         '[pod.retrogradeorbit.bootleg.mustache :as mustache]
         '[pod.retrogradeorbit.bootleg.yaml :as yaml]
         '[pod.retrogradeorbit.bootleg.utils :as utils]
         '[pod.retrogradeorbit.bootleg.selmer :as selmer]
         '[pod.retrogradeorbit.bootleg.glob :as glob]
         '[pod.retrogradeorbit.bootleg.json :as json]
         )

(assert
 (=
  (markdown/markdown "test/files/simple.md")
  '([:h1 "Markdown support"] [:p "This is some simple markdown"])))

(assert
 (=
  (yaml/yaml* "examples/quickstart/fields.yml")
  {:title "Bootleg", :author "Crispin", :body "I'm going to rewrite all my sites with this!"}))

(assert
 (=
  (mustache/mustache "examples/quickstart/quickstart.html"
                     (assoc (yaml/yaml* "examples/quickstart/fields.yml")
                            :body (markdown/markdown "examples/quickstart/simple.md" :html)))
  '([:h1 "Bootleg"] "\n"
    [:h2 "by Crispin"] "\n"
    [:div [:h1 "Markdown support"]
     [:p "This is some simple markdown"]]
    "\n")))

(assert
 (=
  (-> (mustache/mustache "examples/quickstart/quickstart.html"
                         (assoc (yaml/yaml* "examples/quickstart/fields.yml")
                                :body (markdown/markdown "examples/quickstart/simple.md" :html)))
      (utils/convert-to :hickory-seq))
  '({:type :element
     :attrs nil
     :tag :h1
     :content ["Bootleg"]} "\n"
    {:type :element
     :attrs nil
     :tag :h2
     :content ["by Crispin"]} "\n"
    {:type :element
     :attrs nil
     :tag :div
     :content [{:type :element
                :attrs nil
                :tag :h1
                :content ["Markdown support"]}
               {:type :element
                :attrs nil
                :tag :p
                :content ["This is some simple markdown"]}]} "\n")))

(assert
 (=
  (utils/convert-to [:Link "foo"] :xml)
  "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Link>foo</Link>"))

(assert
 (=
  (selmer/selmer "<p>Hello {{name|capitalize}}!</p>" {:name "world"} :data)
  '([:p "Hello World!"])
  ))

(assert
 (=
  (into #{} (glob/glob "**/*.y?l"))
  #{".github/workflows/clojure.yml"
    "test/files/glob/1/test.yml"
    ".circleci/config.yml"
    "test/files/simple.yml"
    "test/files/glob/2/test.yml"
    "test/files/glob/1/test2.yml"
    "examples/quickstart/fields.yml"}))

(assert
 (=
  (json/json "{\"a\": 1, \"b\": [\"a\",\"b\"]}" :data)
  {:a 1, :b ["a" "b"]}))
