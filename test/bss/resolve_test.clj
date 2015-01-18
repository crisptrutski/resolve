(ns bss.resolve-test
  (:require [clojure.test :refer :all]
            [bss.resolve.core :refer :all]
            [peridot.core :refer :all]))

;; integration suite

(def app (get-in (run 9003) [:ring-app :handler]))

(defn ok? [result]
  (= 200 (get-in result [:response :status])))

(defn <-edn [result]
  (clojure.edn/read-string (get-in result [:response :body])))

(deftest test-register-resolve
  (testing "a bunch of things"
    (let [s (session app)]
      (is (ok?
           (request s "/registry/staging.auth"
                    :request-method :put
                    :params {:version "0.0.1"
                             :host    "devbox3"
                             :port    9062})))
      (is (ok?
           (request s "/registry/self"
                    :request-method :put
                    :params {:version "0.0.3"
                             :host    "localhost"
                             :port    9003})))
      (is (ok?
           (request s "/registry/self"
                    :request-method :put
                    :params {:version "0.0.6"
                             :host    "localhost"
                             :port    9002})))
      (is (= {:service-name "self", :versions ["0.0.3" "0.0.6"]}
             (<-edn (request s "/registry/self/versions")))))))
