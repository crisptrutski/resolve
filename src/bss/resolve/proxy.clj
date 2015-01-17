(ns bss.resolve.proxy
  (:require [clojure.string :as str]
            [tailrecursion.ring-proxy :as ring-proxy]))

(defn proxy-request [request host port]
  ;; TODO: retract :_service-name and :_version params?
  (let [path       (get-in request [:params :*])
        host "localhost"
        port 10555
        base-uri   (:uri request)
        target-uri (format "%s://%s:%s/%s" (name (:scheme request)) host port path)
        wrapper    (ring-proxy/wrap-proxy identity base-uri target-uri)]
    (let [x (wrapper (update-in request [:uri] #(str % "/")))]
      (prn x) x)))
