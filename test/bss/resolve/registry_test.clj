(ns bss.resolve.registry-test
  (:require [bss.resolve.registry :refer :all]
            [clojure.test :refer :all]))

(defn setup []
  (reset! registry {}))

;; TODO: not sure what behaviour should be when adding SNAPSHOT
;; (maybe remove other snapshots with same [maj min patch])?
;; or just remove with same SNAPSHOT, and maintain precedence of snapshot strings

(deftest lifecycle-test
  (testing "It registers and unregisters correctly"
    (setup)
    (let [args ["name" "3.2.1" "localhost" 8080]]
      ;; "not thrown"
      (is (not (apply exists? args)))
      (apply register args)
      (is (apply exists? args))
      (apply unregister args)
      (is (not (apply exists? args))))))

(deftest graceful-test
  (testing "it silently no-ops on redundant calls"
    (setup)
    (let [args ["name" "3.2.1" "localhost" 8080]]
      ;; "not thrown"
      (is (not (apply exists? args)))
      (apply register args)
      (apply register args)
      (is (apply exists? args))
      (apply unregister args)
      (apply unregister args)
      (is (not (apply exists? args))))))

;; TODO: SHOULD SUPPORT VERSION WILDCARDS
(deftest exists?-test
  (testing "non-precise matches"
    (setup)
    (register "a" "0.0.1" "cluster1" 8081)
    (is (exists? "a"))
    (is (not (exists? "b")))
    (is (exists? "a" "0.0.1"))
    (is (not (exists? "a" "0.0.2")))
    (is (exists? "a" "0.0.1" "cluster1"))
    (is (not (exists? "a" "0.0.1" "cluster2")))))

(deftest version-for-test
  (testing "basic expectations"
    (setup)
    (register "a" "0.0.1" "cluster1" 8080)
    (register "a" "0.1.0" "cluster2" 8080)
    (register "b" "1.0.1" "cluster1" 8081)
    (register "b" "1.0.2" "cluster2" 8081)
    (register "b" "1.1.0" "cluster1" 8082)
    (is (= #{"0.0.1" "0.1.0"} (versions-for "a")))
    (is (= #{"1.0.1" "1.0.2" "1.1.0"} (versions-for "b")))))

(deftest lookup-test
  (testing "serves appropriate endpoint"
    (setup)
    (register "a" "0.0.1" "cluster1" 8080)
    (register "a" "0.0.2" "cluster2" 8080)
    (register "a" "0.1.1" "cluster1" 8081)
    (register "a" "0.1.1" "cluster1" 8082)
    (register "a" "0.2.3" "cluster2" 8081)
    (register "a" "2.1.0" "cluster1" 8083)
    (is #{} (lookup "b"))
    (is #{} (lookup "b" "0.0.1"))
    (is #{(create-endpoint "cluster1" 8083)}
        (lookup "a"))
    (is #{(create-endpoint "cluster1" 8080)}
        (lookup "a" "0.0.1"))
    (is #{(create-endpoint "cluster1" 8080)}
        (lookup "a" "^0.0.1"))
    (is #{(create-endpoint "cluster2" 8080)}
        (lookup "a" "0.0.2"))
    (is #{(create-endpoint "cluster1" 8081)
          (create-endpoint "cluster1" 8082)}
        (lookup "a" "0.1.1"))
    (is #{(create-endpoint "cluster2" 8081)}
        (lookup "a" "^0.1.0"))
    (is #{}
        (lookup "a" "^0.3.0"))))
