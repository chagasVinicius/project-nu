(ns project-nu.handler-test
  (:use ring.middleware.json)
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [project-nu.handler :refer :all]
            [cheshire.core :as json]
            [cheshire.parse :as parse]))

;;Operation tests
(deftest test-routes

  ;;Test GET / accounts list
  (testing "GET / accounts list"
    (let [response (app (mock/request :get "/accounts-id"))]
      (is (= (:status response) 200))
      (is (= (:body response) (json/encode {:available-accounts []})))
      )
    )

  ;;Test POST /operation with correct input :acc 1
  (testing "POST /operation correct input :acc 1"
    (let [response
          (app
           (mock/content-type (mock/body
                               (mock/request :post "/operation")
                               (json/encode {:id "1" :description "Deposit"
                                                 :amount "10.55" :date "15/10"})
                               )
                              "application/json")
           )]
      (is (= (:status response) 200))
      (is (= (get-in response [:body]) (binding [parse/*use-bigdecimals?* true]
                                         (json/encode {:acc "1" :operation
                                                       {:description "Deposit"
                                                        :amount 10.55}})))
          )
      (is (= (get-in response [:headers "Content-Type"]) "application/json"))
      )
    )

  ;;Test POST /operation with correct input :acc 2
  (testing "POST /operation correct input :acc 2"
    (let [response
          (app
           (mock/content-type (mock/body
                               (mock/request :post "/operation")
                               (json/encode {:id "2" :description "Purchase"
                                             :amount "5.54" :date "17/10"})
                               )
                              "application/json")
           )]
      (is (= (:status response) 200))
      (is (= (get-in response [:body]) (binding [parse/*use-bigdecimals?* true]
                                         (json/encode {:acc "2" :operation
                                                       {:description "Purchase"
                                                        :amount 5.54}})))
          )
      (is (= (get-in response [:headers "Content-Type"]) "application/json"))
      )
    )

  ;;Test POST /operation with a missing argument ':description'
  (testing "POST /operation incorrect input missing argument"
    (let [response
          (app
           (mock/content-type (mock/body
                               (mock/request :post "/operation")
                               (json/encode {:id "1" :amount "10.00"
                                                 :date "17/10"})
                               )
                              "application/json")
           )]
      (is (= (:status response) 404))
      (is (= (get-in response [:body]) (json/encode {:missing-arguments
                                                         ["description"]})))
      (is (= (get-in response [:headers "Content-Type"]) "application/json"))
      )
    )

  ;;Test POST /operation with incorrect valeu for the ':description' argument
  (testing "POST /operation incorrect value ':description'"
    (let [response
          (app
           (mock/content-type (mock/body
                               (mock/request :post "/operation")
                               (json/encode {:id "1" :description "Foo"
                                                 :amount "10.00"
                                                 :date "17/10"})
                               )
                              "application/json")
           )]
      (is (= (:status response) 404))
      (is (= (get-in response [:body]) (json/encode {:unknow-operation "Foo"
                                                         :known-operations
                                                         [#{:deposit :salarie :credit}
                                                          #{:purchase :withdrawal
                                                            :debit}]})))
      (is (= (get-in response [:headers "Content-Type"]) "application/json"))
      )
    )

  ;;Test POST /operation with incorrect valeu for the ':amount' argument
  (testing "POST /operation incorrect input ':amount'"
    (let [response
          (app
           (mock/content-type (mock/body
                               (mock/request :post "/operation")
                               (json/encode {:id "1" :description "Deposit"
                                                 :amount "aaa"
                                                 :date "17/10"})
                               )
                              "application/json")
           )]
      (is (= (:status response) 404))
      (is (= (get-in response [:body]) (json/encode
                                        {:wrong-values ["amount" "aaa"]})))
      (is (= (get-in response [:headers "Content-Type"]) "application/json"))
      )
    )

    ;;Test POST /operation with incorrect valeu for the ':date' argument
  (testing "POST /operation incorrect input ':date'"
    (let [response
          (app
           (mock/content-type (mock/body
                               (mock/request :post "/operation")
                               (json/encode {:id "1" :description "Deposit"
                                                 :amount "10"
                                                 :date "17/1"})
                               )
                              "application/json")
           )]
      (is (= (:status response) 404))
      (is (= (get-in response [:body]) (json/encode "Invalid date format: 17/1")))
      (is (= (get-in response [:headers "Content-Type"]) "application/json"))
      )
    )
  ;;Test GET /account-balance
  (testing "GET /account-balance"
    (let [new-insertion (app
                         (mock/content-type (mock/body
                                             (mock/request :post "/operation")
                                             (json/encode {:id "1"
                                                           :description "Deposit"
                                                           :amount "10.13"
                                                           :date "18/10"})
                                             )
                                            "application/json")
                         )
          response (app (mock/content-type (mock/body
                                            (mock/request :get "/account-balance")
                                            (json/encode {:id "1"})
                                            )
                                           "application/json")
                        )]
      (is (= (:status response) 200))
      (is (= (:body response) (json/encode {:current-balance
                                            {:balance 20.68
                                             :last-operation "18/10"}})))
      )
    )

  ;;Test GET /account-balance with wrong id
  (testing "GET /account-balance"
    (let [response (app (mock/content-type (mock/body
                                            (mock/request :get "/account-balance")
                                            (json/encode {:id "3"})
                                            )
                                           "application/json")
                        )]
      (is (= (:status response) 404))
      (is (= (:body response) (json/encode {:acc-not-found {:acc "3"}})))
      )
    )

  ;;Test GET /account-bank-statement :acc 1
  (testing "GET /account-bank-statement :acc 1"
    (let [new-insertion (app
                         (mock/content-type (mock/body
                                             (mock/request :post "/operation")
                                             (json/encode {:id "1"
                                                               :description "Purchase"
                                                               :amount "3.12"
                                                               :date "18/10"})
                                             )
                                            "application/json")
                         )
          response (app (mock/content-type (mock/body
                                            (mock/request
                                             :get "/account-bank-statement")
                                            (json/encode {:id "1"})
                                            )
                                           "application/json")
                        )]
      (is (= (:status response) 200))
      (is (= (:body response) (json/encode [{:date "15/10"
                                                 :operation [{:operation "Deposit"
                                                              :amount 10.55 :op-id 1}]
                                                 :balance 10.55}
                                                {:date "18/10"
                                                 :operation [{:operation "Deposit"
                                                              :amount 10.13 :op-id 3}
                                                             {:operation "Purchase"
                                                              :amount 3.12 :op-id 4}]
                                                 :balance 17.56}])))
      )
    )

  ;;Test GET /account-bank-statement :acc 2
  (testing "GET /account-bank-statement :acc 2"
    (let [response (app (mock/content-type (mock/body
                                            (mock/request
                                             :get "/account-bank-statement")
                                            (json/encode {:id "2"})
                                            )
                                           "application/json")
                        )]
      (is (= (:status response) 200))
      (is (= (:body response) (json/encode [{:date "17/10"
                                                 :operation [{:operation "Purchase"
                                                              :amount 5.54 :op-id 2}]
                                                 :balance -5.54}])))
      )
    )

  ;;Test GET /accoun-negative-periods with an end
  (testing "GET /account-negative-periods :acc 2"
    (let [new-insertion (app
                         (mock/content-type (mock/body
                                             (mock/request :post "/operation")
                                             (json/encode {:id "2"
                                                               :description "Deposit"
                                                               :amount "10.04"
                                                               :date "21/10"})
                                             )
                                            "application/json")
                         )
          response (app (mock/content-type (mock/body
                                            (mock/request
                                             :get "/account-negative-periods")
                                            (json/encode {:id "2"})
                                            )
                                           "application/json")
                        )]
      (is (= (:status response) 200))
      (is (= (:body response) (json/encode [{:Principal 5.54 :start "17/10"}
                                                {:end "20/10"}])))
      )
    )

  ;;Test GET /account-negative-periods without an end
  (testing "GET /account-negative-periods :acc 2"
    (let [new-insertion (app
                         (mock/content-type (mock/body
                                             (mock/request :post "/operation")
                                             (json/encode {:id "2"
                                                               :description "Withdrawal"
                                                               :amount "15.05"
                                                               :date "23/10"})
                                             )
                                            "application/json")
                         )
          response (app (mock/content-type (mock/body
                                            (mock/request
                                             :get "/account-negative-periods")
                                            (json/encode {:id "2"})
                                            )
                                           "application/json")
                        )]
      (is (= (:status response) 200))
      (is (= (:body response) (json/encode [{:Principal 5.54 :start "17/10"}
                                                {:end "20/10"}
                                                {:Principal 10.55 :start "23/10"}])))
      )
    )

   ;;Test GET / Bank statement with all accounts operations
  (testing "GET /bank-statement"
    (let [response (app (mock/request :get "/bank-daily-balance"))]
      (is (= (:status response) 200))
      (is (= (:body response) (binding [parse/*use-bigdecimals?* true]
                                (json/encode [{"date" "15/10", "operation"
                                               [{"operation" "Deposit",
                                                 "amount" 10.55, "op-id" 1}],
                                               "balance" 10.55}
                                              {"date" "17/10", "operation"
                                               [{"operation" "Purchase",
                                                 "amount" 5.54, "op-id" 2}],
                                               "balance" 5.01}
                                              {"date" "18/10", "operation"
                                               [{"operation" "Deposit",
                                                 "amount" 10.13, "op-id" 3}
                                                {"operation" "Purchase",
                                                 "amount" 3.12, "op-id" 4}],
                                               "balance" 12.02}
                                              {"date" "21/10", "operation"
                                               [{"operation" "Deposit",
                                                 "amount" 10.04, "op-id" 5}],
                                               "balance" 22.06}
                                              {"date" "23/10", "operation"
                                               [{"operation" "Withdrawal",
                                                 "amount" 15.05, "op-id" 6}],
                                               "balance" 7.01}]))))
          )
    )
  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))
