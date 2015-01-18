(ns bss.resolve.redis-registry-test
  (:require [bss.resolve.redis-registry :refer :all]
            [bss.resolve.registry :as reg]
            [taoensso.carmine :as r]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]))

(def db-spec {:spec {:db 9}})

(deftest dual-sync-test
  (testing "peers stay in sync"
    (r/wcar db-spec (r/flushdb))

    (let [reg1 (component/start (map->RedisRegistry {:spec db-spec}))
          reg2 (component/start (map->RedisRegistry {:spec db-spec}))]

      (try
        (reg/register reg1 "a" "0.0.1" "localhost" 2041)
        (Thread/sleep 3)
        (is (= {"a" {"0.0.1" #{{:host "localhost", :port 2041}}}}
               @(:cache reg1)
               @(:cache reg2)))


        (reg/register reg2 "a" "0.0.1" "localhost" 2031)
        (Thread/sleep 3)
        (is (= {"a" {"0.0.1" #{{:host "localhost", :port 2041}
                               {:host "localhost", :port 2031}}}}
               @(:cache reg1)
               @(:cache reg2)))

        (reg/unregister reg2 "a" "0.0.1" "localhost" 2041)
        (Thread/sleep 3)
        (is (= {"a" {"0.0.1" #{{:host "localhost", :port 2031}}}}
               @(:cache reg1)
               @(:cache reg2)))

        (reg/unregister reg1 "a" "0.0.1" "localhost" 2031)
        (Thread/sleep 3)
        (is (= {}
               @(:cache reg1)
               @(:cache reg2)))

        (finally
          (component/stop reg1)
          (component/stop reg2))))))
