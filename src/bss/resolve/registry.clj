(ns bss.resolve.registry
  (:require [bss.resolve.versions :refer [normalize]]))

;; crude storage
;; {name => {version => [{host port}]}}

(defonce registry (atom {}))

(defn exists? [service-name version host port])

;; gracefully handle already-existing (no-op)
(defn register [service-name version host port]
  (swap! registry update-in
         [service-name (normalize version)]
         #(conj % {:host host, :port port})))

;; gracefully handle non-existing (no-op)
(defn unregister [service-name version host port]
  (swap! registry update-in
         [service-name (normalize version)]
         #(remove #{{:host host, :port port}} %)))
