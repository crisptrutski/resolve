(ns bss.resolve-test
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [bss.resolve.core :refer :all]
            [taoensso.carmine :as r]
            [peridot.core :refer :all]))

;; integration suite

(defonce app nil)

(def port 9003)

(def redis-conf {:spec {:db 9}})

(defn setup! []
  (r/wcar redis-conf (r/flushdb))
  (alter-var-root #'app (fn [_] (get-in (run port redis-conf)
                                     [:ring-app :handler]))))

(defn- ok? [result]
  (= 200 (get-in result [:response :status])))

(defn- <-edn [result]
  (let [s (get-in result [:response :body])]
    (edn/read-string (if (string? s) s (slurp s)))))

(deftest test-register-resolve
  (testing "a bunch of things"
    (setup!)
    (let [s (session app)]
      (is (ok? (request s "/registry/staging.auth"
                        :request-method :put
                        :params {:version "0.0.1"
                                 :host    "devbox3"
                                 :port    9062})))
      (is (ok? (request s "/registry/self"
                        :request-method :put
                        :params {:version "0.1.3"
                                 :host    "devbox2"
                                 :port    9001})))
      (is (ok? (request s "/registry/self"
                        :request-method :put
                        :params {:version "0.1.6"
                                 :host    "localhost"
                                 :port    port})))
      (is (= {:service-name "self", :versions ["0.1.3" "0.1.6"]}
             (<-edn (request s "/registry/self/versions"))))
      (is (= {:service-name "self", :end-points [{:host "localhost", :port (str port)}]}
             ;; OH NO, CARET NOT LEGAL
             (<-edn (request s "/lookup/self/_0.1.2"))))
      (is (= {:service-name "staging.auth", :versions ["0.0.1"]}
             (<-edn (request s "/proxy/self/_0.1.3/registry/staging.auth/versions"))))
      (is (ok? (request s "/registry/self"
                        :request-method :delete
                        :params {:version "0.1.6"
                                 :host    "localhost"
                                 :port    port})))
      (is (= {:service-name "self", :versions ["0.1.3"]}
             (<-edn (request s "/registry/self/versions"))))
      (is (= {:service-name "self", :end-points [{:host "devbox2", :port "9001"}]}
             ;; OH NO, CARET NOT LEGAL
             (<-edn (request s "/lookup/self/_0.1.2")))))))
