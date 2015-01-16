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

(def versions
  (map expand-version ["1.2.3"
                       "3.2.1"
                       "3.1.2"
                       "2.3.1"]))

(deftest match-test
  (testing "use precise version if it exists"
    (is (= "2.3.1"
           (match versions "2.3.1"))))
  (testing "return nothing if precise version not found"
    (is (= nil
           (match versions "2.5.1"))))
  (testing "uses latest version if none supplied"
    (is (= "3.2.1"
           (match versions)))))
