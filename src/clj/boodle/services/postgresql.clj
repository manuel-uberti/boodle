(ns boodle.services.postgresql
  (:require
   [boodle.utils.dates :as dates]
   [boodle.utils.exceptions :as exceptions]
   [cheshire.core :as cheshire]
   [clojure.string :as s]
   [honeysql.core :as honey]
   [honeysql.format :as fmt]
   [java-time.local :as jl]
   [java-time.pre-java8 :as jp]
   [next.jdbc :as jdbc]
   [next.jdbc.connection :as connection]
   [next.jdbc.prepare :as prepare]
   [next.jdbc.result-set :as result-set]
   [taoensso.timbre :as log])
  (:import
   (clojure.lang IPersistentMap IPersistentVector)
   (com.zaxxer.hikari HikariDataSource)
   (java.sql Date Timestamp)
   (org.postgresql.util PGobject)))

(extend-protocol result-set/ReadableColumn
  Date
  (read-column-by-index [v _ _]
    (-> v
        jl/local-date
        dates/format-date))

  Timestamp
  (read-column-by-index [v _ _]
    (-> v
        jl/local-date
        dates/format-date))

  PGobject
  (read-column-by-index [pgobj _metadata _index]
    (let [type (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "json" (cheshire/parse-string value true)
        "jsonb" (cheshire/parse-string value true)
        "citext" (str value)
        value))))

(defn to-pg-json
  [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (cheshire/generate-string value))))

(extend-protocol prepare/SettableParameter
  IPersistentMap
  (set-parameter [v s i]
    (.setObject s i (to-pg-json v)))

  IPersistentVector
  (set-parameter [v s i]
    (.setObject s i (to-pg-json v)))

  java.time.LocalDateTime
  (set-parameter [v s i]
    (.setObject s i (jp/sql-timestamp v)))

  java.time.LocalDate
  (set-parameter [v s i]
    (.setObject s i (jp/sql-timestamp v))))

(def datasource (atom nil))

(defn connect!
  [config]
  (let [db-spec (:postgresql config)
        ^HikariDataSource ds (connection/->pool HikariDataSource db-spec)]
    (reset! datasource ds)))

(defn disconnect!
  []
  (when @datasource
    (reset! datasource nil)))

(defn snake-case->kebab-case
  [column]
  (when (keyword? column)
    (keyword (s/replace (name column) #"_" "-"))))

(defn format-output-keywords
  "Convert `output` keywords from snake_case to kebab-case."
  [output]
  (reduce-kv (fn [m k v]
               (assoc m (snake-case->kebab-case k) v))
             {}
             output))

(defmethod fmt/fn-handler "ilike" [_ col qstr]
  (str (fmt/to-sql col) " ilike " (fmt/to-sql qstr)))

(defmethod fmt/fn-handler "not-ilike" [_ col qstr]
  (str (fmt/to-sql col) " not ilike " (fmt/to-sql qstr)))

(defmethod fmt/fn-handler "is-true" [_ col qstr]
  (str (fmt/to-sql col) " is " (fmt/to-sql qstr) " true"))

(defmethod fmt/fn-handler "not-true" [_ col qstr]
  (str (fmt/to-sql col) " is not " (fmt/to-sql qstr) " true"))

(defn execute!
  "Execute query (select/insert/update/delete) using the map in `sqlmap`."
  [sqlmap]
  (let [q (honey/format sqlmap)]
    (try
      (jdbc/execute! @datasource q)
      (catch Exception e
        (log/error (exceptions/stacktrace e))
        (throw (ex-info "Exception in execute!" {:sqlmap sqlmap :query q}))))))

(defn query
  "Run a SELECT query using the map in `sqlmap` and format output keywords."
  [sqlmap]
  (->> (execute! sqlmap)
       (map format-output-keywords)))
