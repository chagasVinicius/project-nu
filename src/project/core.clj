(ns project.core
  (:require [project.operations :as  nu-ops]
            [project.auxiliar   :as  nu-aux]
            [project.account    :as  nu-acc]))

(defn test-insert-op [json-body accounts date-format-op operation-types id-ops]
  "This function gets the json-body input and tests if the arguments are valid.
   After the tests, 'test-insert-op' records the operation in the respective
   account reference and organizes the account operations to avoid a time-lapse.
   Also, the function returns the last operation added to that account."
  (let [op json-body
        check (nu-ops/checking-operations json-body operation-types)]
    (if (not= (:status check) 200)
      check
      (let [new-op (nu-ops/construct-op op date-format-op operation-types id-ops)
            acc-ref (nu-acc/check-create-acc new-op accounts)
            ops-in-ref (filter (fn[x] (= (:id-acc new-op) (:acc @x))) @accounts)
            arranged-ops (nu-ops/org-ops ops-in-ref)]
        (nu-aux/json-response-correct {:acc (:acc arranged-ops) :operation
                                {:description (:desc (first (:ops arranged-ops)))
                                 :amount (:amount(first (:ops arranged-ops)))}}
                               200)))))

(defn filter-acc-ref [id accounts]
  "This function gets an ID and extract the respective account reference from
   all accounts"
  (filter (fn[x] (= (:id id) (:acc @x))) @accounts))

(defn account-balance [id accounts date-format-acc]
  "This function gets an account id and returns the respective account balance"
  (let [acc-ref (filter-acc-ref id accounts)]
    (if (empty? acc-ref)
      (nu-aux/json-response-error {:acc-not-found {:acc (:id id)}} 404)
      (nu-aux/json-response-correct (nu-acc/current-balance acc-ref date-format-acc) 200))))

(defn account-bank-statement [id accounts date-format-acc single]
  "This function gets an account id and returns the respective account bank statement"
  (let [acc-ref (filter-acc-ref id accounts)]
    (if (empty? acc-ref)
      (nu-aux/json-response-error {:acc-not-found {:acc (:id id)}} 404)
      (nu-aux/json-response-correct (nu-acc/daily-balances-ops acc-ref date-format-acc single) 200))))

(defn negative-periods [id accounts pred date-format-acc date-format-op]
  "This function gets an account id and returns the periods when the balance was
   negative (start) and if this state has changed to positive (end)"
  (let [acc-ref (filter-acc-ref id accounts)]
    (if (empty? acc-ref)
      (nu-aux/json-response-error {:acc-not-found {:acc (:id id)}} 404)
      (let [ops-account (nu-acc/get-ops-acc acc-ref "yes")
            balance-ops (nu-acc/get-balances-operation ops-account date-format-acc)
            neg-periods (nu-acc/neg-periods balance-ops pred date-format-acc date-format-op)]
        (nu-aux/json-response-correct neg-periods 200)))))

(defn bank-daily-balance [accounts date-format-acc single]
  "This function gets all accounts and return the daily balance of the bank"
  (let [ops-all-accounts (apply concat (map (fn[x] (:ops @x)) @accounts))]
    (if (empty? ops-all-accounts)
      (nu-aux/json-response-error {:accounts-number 0} 404)
      (let [ops-all-accounts (nu-aux/sort-inverse ops-all-accounts)]
        (nu-aux/json-response-correct (nu-acc/daily-balances-ops ops-all-accounts date-format-acc single)200)))))

(defn accounts-list [accounts]
  "This function gets all accounts and return all available IDs"
  (nu-aux/json-response-correct {:available-accounts (map (fn[x] (:acc @x)) @accounts)}
                         200))
