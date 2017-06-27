(ns project.handler
  (:use compojure.core
        ring.middleware.json)
  (:require [ring.adapter.jetty :as jetty]
            [compojure.handler :as handler]
            [ring.util.response :refer [response]]
            [compojure.route :as route]
            [clj-time.format :as f]
            [project.core :as nu-core])
  (:gen-class))

(def ^:private all-accounts (ref ()))

(def ^:private id-ops (atom 0))

(def ^:private operation-types {"credit" #{:deposit :salarie :credit}
                               "debit" #{:purchase :withdrawal :debit}})

(def ^:private date-op (f/formatter "dd/MM/yyyy"))
(def ^:private date-acc (f/formatter "dd/MM"))


(defroutes app-routes
  (POST "/operation" {body :body} (nu-core/test-insert-op body all-accounts date-op operation-types id-ops))
  (GET "/accounts-id" [] (nu-core/accounts-list all-accounts))
  (GET "/account-balance" {id :body} (nu-core/account-balance id all-accounts date-acc))
  (GET "/account-bank-statement" {id :body} (nu-core/account-bank-statement id all-accounts date-acc "yes"))
  (GET "/account-negative-periods" {id :body} (nu-core/negative-periods id  all-accounts neg? date-acc date-op))
  (GET "/bank-daily-balance" [] (nu-core/bank-daily-balance all-accounts date-acc "no"))
  (route/not-found
   (response {:message "Page not found"})))

(def app
  (-> app-routes
      wrap-json-response
      (wrap-json-body {:keywords? true})
      ))

(defn -main []
  (jetty/run-jetty app {:port 3000}))
