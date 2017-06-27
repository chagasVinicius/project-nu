(ns project-nu.account
  (:require [project-nu.operations :as nu-ops]
            [clj-time.core :as t]
            [clojure.math.numeric-tower :as math]))

(defn new-acc-ref [id]
  "This function gets an ID and creates one account with that ID"
  (ref (merge {:acc id :ops ()})))

;;; add-acc

(defn add-acc [op accounts]
  "This function gets the ID account from an operation,
   creates the respective reference using 'new-acc' and
   adds this new account to the list with all acounts"
  (let [id (:id-acc op)
        new-acc (new-acc-ref id)]
    (dosync (commute accounts conj new-acc))))

(defn add-op [op accounts]
  "This function adds an operation to the respective account"
  (let [id (:id-acc op)
        acc (filter (fn[x] (= (:id-acc op) (:acc @x))) @accounts)
        acc (first acc)]
    (dosync (alter acc update-in [:ops] conj op))))

(defn check-create-acc [op accounts]
  "This function checks the existence of one account and uses the auxiliar functions
   to create a new-one or just adds the operation"
  (let [id (:id-acc op)
        acc (filter (fn[x] (= id (:acc @x))) @accounts)]
    (if (empty? acc)
      (dosync (add-acc op accounts) (add-op op accounts))
      (dosync (add-op op accounts)))))

(defn amount->val [operation]
  "This function changes the amount of one operation based in the type of operation"
  (let [type (first (:type-op operation))
        amount  (:amount operation)]
    (if (= "debit" type)
      (- amount)
      amount)))

(defn get-balances-helper [start-balance ops-acc date-format]
  "This is a helper function to add the balances from all operations
   in a specific date"
  (if (empty? ops-acc)
    ops-acc
    (let [new-balance {:balance (with-precision 5 (+ (:balance start-balance)
                                                     (amount->val (first ops-acc))))
                       :date (nu-ops/format-date (:date (first ops-acc))
                                          date-format "unparse")
                       :op-id (:op-id (first ops-acc))
                       :desc (:desc (first ops-acc))
                       :op-val (:amount (first ops-acc))}]
      (cons new-balance (get-balances-helper new-balance (rest ops-acc) date-format)))))

(defn get-balances-operation [ops-account date-format]
  "This function uses the 'get-balances-helper' to create an account balance for
   each operation"
  (get-balances-helper
   {:balance 0 :date " " :desc "" :op-id 0 :op-val 0}
   ops-account
   date-format))

(defn get-ops-acc [acc-ref single]
  "This function gets an account reference and extracts the reverted ops of
   the account. This can be applied to a single acc-ref ('yes') or multiple."
  (if (= "yes" single)
    (reverse (:ops @(first acc-ref)))
    (reverse acc-ref)))

(defn current-balance [account date-format]
  "This function gets an account and returns the current balance of the account"
  (let [ops-account (get-ops-acc account "yes")
        balance-ops (get-balances-operation ops-account date-format)
        current-balance (last balance-ops)]
    {:current-balance {:balance (:balance current-balance)
                       :last-operation (:date current-balance)}}))

(defn balances-acc-date  [balance-ops date]
  " This function gets the balances for each operation and return the balances for
    each specific date"
  (let [balances-daily (filter (fn[x] (= date (:date x))) balance-ops)]
    (last (map :balance balances-daily))))

(defn get-ops-date [balance-ops date]
  "This function returns the operations realized in a sepecific date"
  (let [ops (filter (fn[x] (= date (:date x))) balance-ops)]
    (map (fn [x] {:operation (:desc x)
                  :amount (:op-val x)
                  :op-id (:op-id x)}) ops)))

(defn arrange-balances [date balance ops]
  "This function creates a map with a date, a group of operations and the respective
   date balance"
  {:date date, :operation ops, :balance balance})

(defn daily-balances-ops [account date-format single]
  "This function gets the operations of one account and returns the balance of each
   date that an operation occurred and return the balance for each date with the
   respective operations"
  (let [ops-account (get-ops-acc account single)
        balance-ops (get-balances-operation ops-account date-format)
        dates (distinct (map :date balance-ops))]
    (for [date dates]
      (partial (arrange-balances
                date
                (balances-acc-date balance-ops date)
                (get-ops-date balance-ops date))))))

(defn attrs-neg [balance-day date-format-acc date-format-op]
  "This function extracts the attributes of a daily-balance in accordance with the
   value of the balance (pos or neg)"
  (cond
    ;;Test case 1
    (neg? (:balance balance-day))
    {:Principal (math/abs (:balance balance-day)) :start (:date balance-day)}
    ;;Test case 2
    (pos? (:balance balance-day))
    {:end  (nu-ops/format-date
            (t/minus (nu-ops/format-date (:date balance-day) date-format-op "parse")
                     (t/days 1)
                     )
            date-format-acc "unparse")}
    :else {:end nil}))

(defn neg-periods [balance-ops pred date-format-acc date-format-op]
  "This function gets the daily balances with operations
   and returns the start date of a predicate (neg?), switch this predicate to the
   opposite and recursively continues with these searchs and changes"
  (if (empty? balance-ops)
    balance-ops
    (if (pred (:balance (first balance-ops)))
      (cons (attrs-neg (first balance-ops) date-format-acc date-format-op)
            (neg-periods (rest balance-ops) (complement pred)
                         date-format-acc date-format-op))
      (neg-periods (rest balance-ops) pred date-format-acc date-format-op))))
