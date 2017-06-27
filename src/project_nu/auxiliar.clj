(ns project-nu.auxiliar
  (:require [clj-time.core :as t]
            [cheshire.core :refer :all]))

(defn halve [a-seq]
  "This function split a sequence in two equal parts"
  (let [n (int (/ (count a-seq) 2))]
    [(take n a-seq) (drop n a-seq)]))

(defn seq-merge [a-seq b-seq]
  "This function gets two sequences and merge then in a sorted one"
  (cond
    (empty? a-seq) b-seq
    (empty? b-seq) a-seq
    :else
    (if (or (t/after? (:date (first a-seq)) (:date (first b-seq)))
            (t/equal? (:date (first a-seq)) (:date (first b-seq))))
    (cons (first a-seq) (seq-merge (rest a-seq) b-seq))
    (cons (first b-seq) (seq-merge a-seq (rest b-seq))))))

(defn sort-inverse [a-seq]
  "This function sorts a sequence in an inverse order"
  (let [[a b] (halve a-seq)]
    (if (or (= 1 (count a)) (= 1 (count b)))
      (seq-merge a b)
      (seq-merge (sort-inverse a) (sort-inverse b)))))

(defn json-response-correct [data status]
  "This function creates a standard JSON correct response. This response
   contains a data to be showed and the status"
  {:status status
   :headers {"Content-Type" "application/json"}
   :body (generate-string data)})

(defn json-response-error [data status]
  " This function creates a standard JSON error response. This response
    contains a data to be showed and the status"
  {:status status
   :headers {"Content-Type" "application/json"}
   :body (generate-string data)})
