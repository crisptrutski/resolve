(ns bss.resolve.proxy)

(defn proxy-request [request host port]
  (let [path (get-in request [:request-params :*])]
    ;; TODO: hopefully we can just reuse code here to dispatch
    ;; TODO: rewrite route
    ;; TODO: retract :_service-name and :_version params?
    (prn request host port)
    "OK"))
