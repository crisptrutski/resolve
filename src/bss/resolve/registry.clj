(ns bss.resolve.registry
  (:require [bss.resolve.versions :refer [normalize]]))

;; crude storage
;; {name => {version => #{ {host port}, ... }}}

(defonce registry (atom {}))

(defn- create-entry [host port]
  {:host host, :port port})

(defn exists?
  "Determines whether there are any matches, with match-all for missing params"
  ([service-name]
   (contains? @registry service-name))
  ;; TODO: this should allow caret and wildcard ranges
  ([service-name version]
   (contains? (get @registry service-name) version))
  ([service-name version host]
   (some (comp #{host} :host)
         (get-in @registry [service-name (normalize version)])))
  ([service-name version host port]
   (some #{(create-entry host port)}
         (get-in @registry [service-name (normalize version)]))))

;; gracefully handle already-existing (no-op)
(defn register
  "Deregister {host,port} as an endpoint offering service@version"
  [service-name version host port]
  (swap! registry update-in
         [service-name (normalize version)]
         #(conj (set %) (create-entry host port))))

;; gracefully handle non-existing (no-op)
(defn unregister
  "Reverse `register`"
  [service-name version host port]
  (swap! registry update-in
         [service-name (normalize version)]
         #(disj % (create-entry host port))))

(defn versions-for
  "Determines set of versions available for given service-name"
  [service-name]
  (set (keys (get @registry service-name))))
