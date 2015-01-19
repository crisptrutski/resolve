(ns bss.resolve.client
  (:require #_[clj-http.client :refer [request]]
            [bss.resolve.redis-registry :as rr]
            [bss.resolve.registry :as reg]
            [com.stuartsierra.component :as component]))

(defn- rand-set
  "Pick random element from set"
  [set]
  (-> set seq rand-nth))

(defn url-for
  "Synchronous, provides a endpoint from matched set (if any exist), otherwise
  provide a proxying route"
  [registry service-name version]
  (or
   ;; TODO: should possibly allow passing metadata used to support stickyness, etc
   ;;       and defer to specific strategy for choosing endpoint, rather than rand-set
   (rand-set (reg/lookup registry service-name version))
   ;; TODO should provide registry base URI here (via config?)
   (str "/proxy/" service-name "/" version)))

(defn with-url
  "Asynchronous version, trades performances for robustness.
  Still no guarantee - endpoint can go down any time after delivery"
  [registry service-name version]
  (let [p (promise)]
    (if-let [e (rand-set (reg/lookup registry service-name version))]
      (deliver p e)
      ;; TODO: deliver as soon as a service that supports this [name, version] is
      ;;       registered. this requires adding some more state to registries.
      ;;       eg. (reg/register-promise registry service-name version p)
      (throw (UnsupportedOperationException. "Not implemented yet")))
    p))


;; TODO: should really be a lightweight peer (see gossip story)
;;       for now just exposing redis conn string via API allows
;;       this to create a syncing peer
(defn get-registry [uri]
  (component/start (rr/map->RedisRegistry {})))

(defn register [{:keys [service-name
                        service-registry
                        host
                        port
                        version] :as config}]
  (let [registry (get-registry service-registry)]
    (if-not service-registry
      (println "No service registry defined, not registering self")
      (let [missing-keys (remove #(get config %) [:service-name :port :version])]
        (println "Registering service")
        (if (seq missing-keys)
          (throw (RuntimeException. (str "Missing required keys: " (into [] missing-keys))))
          (reg/register registry
                        service-name
                        version
                        host
                        port))))))

(defn unregister [{:keys [service-name
                          service-registry
                          host
                          port
                          version] :as config}]
  (let [registry (get-registry service-registry)]
    (if-not service-registry
      (println "No service registry defined, nothing to deregister")
      (when (some #(get config %) [:service-name :port :version])
        (println "Unregistering service")
        (reg/unregister registry
                        service-name
                        version
                        host
                        port)))))

(defrecord AutoRegistry [service-name service-registry host port version]
  component/Lifecycle
  (start [this] (register this) this)
  (stop [this] (unregister this) this))

(defn auto-registry [conf]
  (map->AutoRegistry conf))
