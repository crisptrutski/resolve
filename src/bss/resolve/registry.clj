(ns bss.resolve.registry
  (:require [bss.resolve.versions :as v]
            [com.stuartsierra.component :as component]))

(defrecord Registry [cache]
  component/Lifecycle
  (start [component]
    ;; crude storage: {name => {version => #{ {host port}, ... }}}
    (assoc component :cache (or cache (atom {}))))
  (stop [component]))

(defn create-endpoint [host port]
  {:host host, :port port})

(defn cleanup!
  "Remove path if no endpoints left"
  [cache [name ver :as path]]
  (when (empty? (get-in @cache path))
    (if (= 1 (count (get @cache name)))
      (swap! cache dissoc name)
      (swap! cache update-in [name] #(dissoc % ver)))))

(defn- exists?*
  "Determines whether there are any matches, with match-all for missing params"
  ([registry service-name]
   (contains? registry service-name))
  ([registry service-name version]
   (v/match (map v/expand-version (keys (get registry service-name))) version))
  ([registry service-name version host]
   (boolean
    (some (comp #{host} :host)
          (get-in registry [service-name (v/normalize version)]))))
  ([registry service-name version host port]
   (boolean
    (some #{(create-endpoint host port)}
          (get-in registry [service-name (v/normalize version)])))))

(defn exists?
  "Determines whether there are any matches, with match-all for missing params"
  [{:keys [cache]} & args]
  (apply exists?* @cache args))

;; gracefully handle already-existing (no-op)
(defn register
  "Deregister {host,port} as an endpoint offering service@version"
  [{:keys [cache]} service-name version host port]
  (swap! cache update-in
         [service-name (v/normalize version)]
         #(conj (set %) (create-endpoint host port))))

;; gracefully handle non-existing (no-op)
(defn unregister
  "Reverse `register`"
  [{:keys [cache]} service-name version host port]
  (let [path [service-name (v/normalize version)]]
    (swap! cache update-in path
           #(disj % (create-endpoint host port)))
    (cleanup! cache path)))

(defn- versions-for*
  "Determines set of versions available for given service-name"
  [registry service-name]
  (set (keys (get registry service-name))))

(defn versions-for [{:keys [cache]} service-name]
  (versions-for* @cache service-name))

(defn lookup [{:keys [cache]} service-name & [version-pattern]]
  (let [registry @cache]
    (if (contains? registry service-name)
      ;; sucks, versions-for may read different version of the cache
      (let [v-strings (versions-for* registry service-name)
            versions (map v/expand-version v-strings)
            braw (v/match versions version-pattern)
            base (if braw (v/expand-version braw))
            keys (filter #(= base (v/expand-version %)) v-strings)
            ends (mapcat #(get-in registry [service-name %]) keys)]
        (set ends)))))
