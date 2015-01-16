(ns bss.resolve.registry-test
  (:require [bss.resolve.registry :refer :all]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]))

(defn setup []
  (component/start (map->Registry {})))

;; TODO: not sure what behaviour should be when adding SNAPSHOT
;;      (maybe remove other snapshots with same [maj min patch])?
;;      or just remove with same SNAPSHOT, and maintain precedence of
;;      snapshot strings

;; TODO: should probably throw an error if registering an endpoint that already
;;       exists as another service and/or mapping
;;       (OR silently remove the other mapping)


(deftest lifecycle-test
  (testing "It registers and unregisters correctly"
    (let [r (setup)
          args ["name" "3.2.1" "localhost" 8080]]
      ;; "not thrown"
      (is (not (apply exists? r args)))
      (apply register r args)
      (is (apply exists? r args))
      (apply unregister r args)
      (is (not (apply exists? r args))))))

(deftest graceful-test
  (testing "it silently no-ops on redundant calls"
    (let [r (setup)
          args ["name" "3.2.1" "localhost" 8080]]
      ;; "not thrown"
      (is (not (apply exists? r args)))
      (apply register r args)
      (apply register r args)
      (is (apply exists? r args))
      (apply unregister r args)
      (apply unregister r args)
      (is (not (apply exists? r args))))))

(deftest exists?-test
  (testing "non-precise matches"
    (let [r (setup)]
      (register r "a" "0.0.1" "cluster1" 8081)
      (register r "a" "3.2.1" "cluster1" 8081)
      (is (exists? r "a"))
      (is (not (exists? r "b")))
      (is (exists? r "a" "0.0.1"))
      (is (exists? r "a" "^3.1.9"))
      (is (not (exists? r "a" "0.0.2")))
      (is (exists? r "a" "0.0.1" "cluster1"))
      (is (not (exists? r "a" "0.0.1" "cluster2"))))))

(deftest version-for-test
  (testing "basic expectations"
    (let [r (setup)]
      (register r "a" "0.0.1" "cluster1" 8080)
      (register r "a" "0.1.0" "cluster2" 8080)
      (register r "b" "1.0.1" "cluster1" 8081)
      (register r "b" "1.0.2" "cluster2" 8081)
      (register r "b" "1.1.0" "cluster1" 8082)
      (is (= #{"0.0.1" "0.1.0"} (versions-for r "a")))
      (is (= #{"1.0.1" "1.0.2" "1.1.0"} (versions-for r "b"))))))

(deftest lookup-test
  (testing "serves appropriate endpoint"
    (let [r (setup)]
      (register r "a" "0.0.1" "cluster1" 8080)
      (register r "a" "0.0.2" "cluster2" 8080)
      (register r "a" "0.1.1" "cluster1" 8081)
      (register r "a" "0.1.1" "cluster1" 8082)
      (register r "a" "0.2.3" "cluster2" 8081)
      (register r "a" "2.1.0" "cluster1" 8083)
      (is #{} (lookup r "b"))
      (is #{} (lookup r "b" "0.0.1"))
      (is #{(create-endpoint "cluster1" 8083)}
          (lookup r "a"))
      (is #{(create-endpoint "cluster1" 8080)}
          (lookup r "a" "0.0.1"))
      (is #{(create-endpoint "cluster1" 8080)}
          (lookup r "a" "^0.0.1"))
      (is #{(create-endpoint "cluster2" 8080)}
          (lookup r "a" "0.0.2"))
      (is #{(create-endpoint "cluster1" 8081)
            (create-endpoint "cluster1" 8082)}
          (lookup r "a" "0.1.1"))
      (is #{(create-endpoint "cluster2" 8081)}
          (lookup r "a" "^0.1.0"))
      (is #{}
          (lookup r "a" "^0.3.0")))))
