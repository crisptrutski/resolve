(ns bss.resolve.registry-test
  (:require [bss.resolve.registry :refer :all]
            [clojure.test :refer :all]))

(defn setup []
  (reset! registry {}))

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
