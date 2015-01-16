(ns bss.resolve.registry
  (:require [bss.resolve.versions :refer [normalize]]))

;; crude storage
;; {name => {version => #{ {host port}, ... }}}

(defonce registry (atom {}))

(defn- create-entry [host port]
  {:host host, :port port})

(defn exists? [service-name version host port]
  (some
   #{(create-entry host port)}
   (get-in @registry [service-name (normalize version)])))

;; gracefully handle already-existing (no-op)
(defn register [service-name version host port]
  (swap! registry update-in
         [service-name (normalize version)]
         #(conj (set %) (create-entry host port))))

;; gracefully handle non-existing (no-op)
(defn unregister [service-name version host port]
  (swap! registry update-in
         [service-name (normalize version)]
         #(disj % (create-entry host port))))
