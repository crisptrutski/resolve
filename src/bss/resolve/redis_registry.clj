(ns bss.resolve.redis-registry
  (:require [bss.resolve.registry :as registry]
            [taoensso.carmine :as r]
            [clojure.set :as set]
            [com.stuartsierra.component :as component]))

(defn- read-redis [spec]
  (r/wcar spec (r/get "registry")))

(defn- write-redis! [spec data]
  (r/wcar spec (r/set "registry" data)))

(defn flatten-once [hsh]
  (into {} (mapcat (fn [[k1 hsh2]] (map (fn [[k2 v]] [[k1 k2] v]) hsh2)) hsh)))

(defn diff [xs ys]
  (let [xss (flatten-once xs)
        yss (flatten-once ys)]
    (->> (for [k (distinct (concat (keys xss) (keys yss)))
               :let [v1 (xss k), v2 (yss k)]
               :when (not= v1 v2)
               :let [add (set/subset? v1 v2)]]
           [k
            (if add :add :remove)
            (if add
              (set/difference v2 v1)
              (set/difference v1 v2))]))))

(defn- pub-changes! [spec old new]
  (r/wcar spec (doseq [triple (diff old new)]
                 (r/publish "registry-diff" triple))))

(defn- create-redis-atom [spec]
  (let [a (atom (read-redis spec))]
    (add-watch a :sync (fn [_ _ old new]
                         (write-redis! spec new)
                         (pub-changes! spec old new)))
    a))

(defn- update-cache [spec cache msg]
  (let [[type channel payload] msg]
    (when (= type "message")
      (let [[path action endpoints] payload
            f (if (= action :add) set/union set/difference)]
        (swap! cache update-in path f endpoints)
        ;; TODO: remove circular reference
        (registry/cleanup! cache path)))))

(defn- create-pubsub-listener [spec cache]
  (r/with-new-pubsub-listener (:spec spec)
    {"registry-diff" (partial update-cache spec cache)}
    (r/subscribe "registry-diff")))

(defrecord RedisRegistry [spec cache listener]
  component/Lifecycle
  (start [component]
    (let [cache (create-redis-atom spec)]
      (assoc component
        :cache cache
        :listener (create-pubsub-listener spec cache))))
  (stop [component]
    (remove-watch cache :sync)
    (r/close-listener listener)
    (assoc component
      :cache nil
      :listener listener)))
