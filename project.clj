(defproject project-nu "0.1.0-SNAPSHOT"
  :description "Project-nu: A web service to banking accounts"
  :url "http://projec-nu.com/"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.1"]
                 [cheshire "5.7.0"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-core "1.5.1"]
                 [ring/ring-jetty-adapter "1.2.1"]
                 [org.clojure/math.numeric-tower "0.0.4"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler project-nu.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}}
  :aot  [project-nu.handler]
  :main project-nu.handler)
