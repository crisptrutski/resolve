(ns bss.resolve.registry
  (:require [bss.resolve.versions :as v]))

;; crude storage
;; {name => {version => #{ {host port}, ... }}}

;; TODO: move registry atom into component state

(defonce registry (atom {}))

(defn create-endpoint [host port]
  {:host host, :port port})

(defn exists?
  "Determines whether there are any matches, with match-all for missing params"
  ([service-name]
   (contains? @registry service-name))
  ([service-name version]
   (v/match (map v/expand-version (keys (get @registry service-name))) version))
  ([service-name version host]
   (boolean
    (some (comp #{host} :host)
          (get-in @registry [service-name (v/normalize version)]))))
  ([service-name version host port]
   (boolean
    (some #{(create-endpoint host port)}
          (get-in @registry [service-name (v/normalize version)])))))

;; gracefully handle already-existing (no-op)
(defn register
  "Deregister {host,port} as an endpoint offering service@version"
  [service-name version host port]
  (swap! registry update-in
         [service-name (v/normalize version)]
         #(conj (set %) (create-endpoint host port))))

;; gracefully handle non-existing (no-op)
(defn unregister
  "Reverse `register`"
  [service-name version host port]
  (swap! registry update-in
         [service-name (v/normalize version)]
         #(disj % (create-endpoint host port))))

(defn versions-for
  "Determines set of versions available for given service-name"
  [service-name]
  (set (keys (get @registry service-name))))

(defn lookup [service-name & [version-pattern]]
  (if (contains? @registry service-name)
    (let [v-strings (versions-for service-name)
          versions (map v/expand-version v-strings)
          braw (v/match versions version-pattern)
          base (if braw (v/expand-version braw))
          keys (filter #(= base (v/expand-version %)) v-strings)
          ends (mapcat #(get-in @registry [service-name %]) keys)]
      (set ends))))
