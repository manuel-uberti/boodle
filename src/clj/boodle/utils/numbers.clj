(ns boodle.utils.numbers
  (:require [clojure.string :as s]))

(defn en->ita
  [x]
  (-> x
      .toString
      (s/replace #"\." ",")))

(defn convert-amount
  [m k]
  (->> (get m k 0)
       en->ita
       (assoc m k)))

(defn or-zero
  [x]
  (if x
    x
    0))

(defn str->integer
  [s]
  (if (string? s)
    (if (empty? s)
      0
      (Integer/parseInt s))
    s))

(defn strs->integers
  [xs]
  (if (sequential? xs)
    (map str->integer xs)
    (str->integer xs)))

(defn record-str->integer
  [record k]
  (let [s (k record)]
    (if (empty? s)
      (assoc record k 0)
      (->> (s/replace s #"," ".")
           Integer/parseInt
           (assoc record k)))))

(defn record-str->double
  [record k]
  (let [s (k record)]
    (if (empty? s)
      (assoc record k 0)
      (->> (s/replace s #"," ".")
           Double/parseDouble
           (assoc record k)))))

(defn str->double
  [s]
  (if (empty? s)
    0
    (let [n (s/replace s #"," ".")]
      (Double/parseDouble n))))
