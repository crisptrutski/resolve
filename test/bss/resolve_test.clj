(ns bss.resolve-test
  (:require [clojure.test :refer :all]
            [bss.resolve :refer :all]))

;; 1) (un)register service-name, version, host, port
;; 2) Lookup service-name, [version] (see bss.resolve.versions)
;; 3) Proxy deep routes to service (steal from tailrecursion.ring-proxy)
;; 4) Run periodic health checks against services (require convention for health checks)
