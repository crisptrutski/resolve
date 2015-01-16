(ns bss.resolve.versions-test
  (:require [bss.resolve.versions :refer :all]
            [clojure.test :refer :all]))

(deftest expand-version-test
  (testing "all the variations"
    (is (= [3 2 1 ""]
           (expand-version "3.2.1")))
    (is (= [3 2 1 "SNAPSHOT"]
           (expand-version "3.2.1-SNAPSHOT")))
    (is (= [3 2 0 ""]
           (expand-version "3.2")))
    (is (= [3 0 0 ""]
           (expand-version "3")))))

(deftest bump-test
  (testing "basic examples"
    (is (= "0.0.1" (bump "0.0.1-alpha3")))
    (is (= "0.0.2" (bump "0.0.1+amd")))
    (is (= "0.0.2" (bump "0.0.1")))
    (is (= "0.2.0" (bump "0.1.1")))
    (is (= "2.0.0" (bump "1.3.2")))))

(def versions
  (map expand-version ["1.2.3"
                       "3.2.1"
                       "4.3.2"
                       "3.1.2"
                       "2.3.1"]))

(match versions "^3.1.2")

(deftest match-test
  (testing "use precise version if it exists"
    (is (= "2.3.1"
           (match versions "2.3.1"))))
  (testing "return nothing if precise version not found"
    (is (= nil
           (match versions "2.5.1"))))
  (testing "uses latest version if none supplied"
    (is (= "4.3.2"
           (match versions))))
  (testing "semantic versions match the greatest"
    (is (= "3.2.1"
           (match versions "^3.1.2"))))
  (testing "semantic versions match the greatest"
    (is (= nil
           (match versions "^3.2.2")))))
