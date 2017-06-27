(ns project-nu.operations
  (:require [clojure.set          :as set]
            [clj-time.core        :as t]
            [clj-time.format      :as f]
            [project-nu.auxiliar  :as nu-aux]))

;;  This is the basic structure of a JSON body and the keys which are expected as
;;  input
(def ^:private foo-json {:id 1 :description "Descripton" :amount 0.0 :date "dd/MM"})

(defn format-date [date date-format parse-type]
  "This function gets the date input and format this in according with the
   date-format and the type of operation ('parse' or 'unparse')"
  (if (= "unparse" parse-type)
    (f/unparse date-format date)
    (let [size (count (seq date))]
      (if (and (= 10 size) (= "parse" parse-type))
        (f/parse date-format date)
        (f/parse date-format (str (str date) "/" (t/year (t/now))))))))

(defn type-op [desc operation-types]
  "This function gets the description of an operation and set if it is a 'credit'
   or 'debit' type"
  (let [op (first (clojure.string/split desc #"\s"))
        op (keyword (clojure.string/lower-case op))]
    op (keys (filter (fn[x] (contains? (second x)  op)) operation-types))))

(defn values-consistency [json-body operation-types]
  "This function gets the operation after the keys are tested and evaluate if
   the values of each key are consistent with the expected"
  (let [id (read-string (:id json-body))
        amount (read-string (:amount json-body))
        desc (:description json-body)
        date (seq(:date json-body))]
    (cond
      ;;test case 1
      (not (number? id))     (nu-aux/json-response-error {:wrong-values [:id id]} 404)
      ;;test case 2
      (not (number? amount)) (nu-aux/json-response-error {:wrong-values [:amount amount]}
                                                  404)
      ;;test case 3
      (nil? (type-op desc operation-types))
      (nu-aux/json-response-error  {:unknow-operation desc
                             :known-operations [ #{:deposit :salarie :credit}
                                                #{:purchase :withdrawal :debit}]}
                            404)
      ;;test case 4
      (or (not-every? (fn[x] (or (number? (read-string (str x))) (="/" (str x))))date)
          (< (count date) 5)
          )
      (nu-aux/json-response-error (str "Invalid date format: " (:date json-body)) 404)
      :else (nu-aux/json-response-correct json-body 200))))

(defn checking-operations [json-body operation-types]
  "This function checks the validity of keys and values for a new-operation"
  (let [json-keys (set (keys json-body))
        foo-keys (set (keys foo-json))]
    (if (not= json-keys foo-keys)
      (nu-aux/json-response-error {:missing-arguments (set/difference foo-keys json-keys)}
                           404)
      (values-consistency json-body operation-types))))

(defn construct-op [json-body date-format operation-types id-ops]
  "This function gets the JSON input body and creates a standard operation construct
   with the informations"
  {:id-acc (:id json-body)
   :op-id (dosync (swap! id-ops inc))
   :type-op (type-op (:description json-body) operation-types)
   :desc (:description json-body)
   :amount (bigdec (:amount json-body))
   :date (format-date (:date json-body) date-format "parse")})

(defn org-ops [acc-ref-ops]
  "This function sort the ops when a new one is added, this avoid a time lapse in the
   operations"
  (dosync (alter (first acc-ref-ops) update-in [:ops] nu-aux/sort-inverse)))
