(ns bootleg.hiccup
  (:require [bootleg.file :as file]
            [bootleg.utils :as utils]
            [bootleg.markdown :as markdown]
            [bootleg.mustache :as mustache]
            [bootleg.selmer :as selmer]
            [bootleg.html :as html]
            [bootleg.yaml :as yaml]
            [bootleg.json :as json]
            [bootleg.edn :as edn]
            [bootleg.namespaces :as namespaces]
            [bootleg.context :as context]
            [bootleg.glob :as glob]
            [sci.core :as sci]
            [clojure.walk :as walk]))

(defn load-file* [ctx file]
  (let [s (slurp file)]
    (sci/eval-string s ctx)))

(declare process-hiccup)

(defn realize-all-seqs [form]
  (walk/postwalk
   (fn [f]
     (if (seq? f)
       (doall f)
       f))
   form))

(defn process-hiccup-data [path data]
  (let [ctx {
             :namespaces namespaces/namespaces
             :bindings
             {
              ;; file loading
              'markdown markdown/markdown
              'mustache mustache/mustache
              'html html/html
              'hiccup process-hiccup
              'selmer selmer/selmer

              ;; vars files
              'yaml yaml/yaml
              'json json/json
              'edn edn/edn

              ;; directories and filenames
              'glob glob/glob
              'symlink file/symlink
              'mkdir file/mkdir
              'mkdirs file/mkdirs

              ;; testing
              'is-hiccup? utils/is-hiccup?
              'is-hiccup-seq? utils/is-hiccup-seq?
              'is-hickory? utils/is-hickory?
              'is-hickory-seq? utils/is-hickory-seq?

              ;; conversions
              'convert-to utils/convert-to
              'markup-type utils/markup-type
              'as-html utils/as-html

              ;; command line args
              (with-meta '*command-line-args* {:sci.impl/deref! true}) (sci/new-dynamic-var '*command-line-args* *command-line-args*)

              ;; standard in out err
              (with-meta '*in* {:sci.impl/deref! true}) (sci/new-dynamic-var '*in* *in*)
              (with-meta '*out* {:sci.impl/deref! true}) (sci/new-dynamic-var '*out* *out*)
              (with-meta '*err* {:sci.impl/deref! true}) (sci/new-dynamic-var '*err* *err*)
              }
             :imports {'System 'java.lang.System}
             :classes {'java.lang.System System
                       'java.time.Clock java.time.Clock
                       'java.time.DateTimeException java.time.DateTimeException
                       'java.time.DayOfWeek java.time.DayOfWeek
                       'java.time.Duration java.time.Duration
                       'java.time.Instant java.time.Instant
                       'java.time.LocalDate java.time.LocalDate
                       'java.time.LocalDateTime java.time.LocalDateTime
                       'java.time.LocalTime java.time.LocalTime
                       'java.time.Month java.time.Month
                       'java.time.MonthDay java.time.MonthDay
                       'java.time.OffsetDateTime java.time.OffsetDateTime
                       'java.time.OffsetTime java.time.OffsetTime
                       'java.time.Period java.time.Period
                       'java.time.Year java.time.Year
                       'java.time.YearMonth java.time.YearMonth
                       'java.time.ZonedDateTime java.time.ZonedDateTime
                       'java.time.ZoneId java.time.ZoneId
                       'java.time.ZoneOffset java.time.ZoneOffset
                       'java.time.temporal.TemporalAccessor java.time.temporal.TemporalAccessor
                       'java.time.format.DateTimeFormatter java.time.format.DateTimeFormatter
                       'java.time.format.DateTimeFormatterBuilder java.time.format.DateTimeFormatterBuilder
                       }}]
    (context/with-path path
      (-> data
          (sci/eval-string
           (update ctx
                   :bindings assoc 'load-file
                   #(load-file*
                     ctx
                     (file/path-join path %))))
          realize-all-seqs))))

(defn process-hiccup
  ([file]
   (let [fullpath (file/path-join context/*path* file)
         [path file] (file/path-split fullpath)]
     (process-hiccup path file)))
  ([path file]
   (->> file
        (file/path-join path)
        slurp
        (process-hiccup-data path))))
