(ns bss.resolve.web
  (:require [bss.resolve.proxy :as proxy]
            [bss.resolve.registry :as registry]
            [compojure.core :refer [POST DELETE PUT GET ANY routes]]
            [compojure.handler :refer [api]]
            [compojure.route :refer [resources]]
            [ring.middleware.edn :refer [wrap-edn-params]]))

;; TODO: speak JSON on demand

(defn ->edn [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn create-routes [registry]
  (let [lookup (partial registry/lookup registry)
        exists? (partial registry/exists? registry)
        register (partial registry/register registry)
        unregister (partial registry/unregister registry)
        versions-for (partial registry/versions-for registry)]
    (routes

     (PUT "/registry/:service-name" {:keys [params] :as req}
       (let [service-name (get-in req [:route-params :service-name])
             {:keys [version host port]} params]
         (assert (every? identity [service-name version host port]))
         (let [exists (exists? service-name version host port)]
           (when-not exists
             (register service-name version host port))
           (->edn {:success true, :existed exists}))))

     (DELETE "/registry/:service-name" {:keys [params]}
       (let [{:keys [version host port service-name]} params]
         (assert (every? identity [version host port]))
         (let [exists (exists? service-name version host port)]
           (when exists
             (unregister service-name version host port))
           (->edn {:success true, :existed exists}))))

     (GET "/registry/:service-name/versions" [service-name]
       (->edn {:service-name service-name
               :versions (vec (sort (versions-for service-name)))}))

     (GET "/lookup/:service-name/:version" [service-name version]
       (->edn {:service-name service-name
               :end-points (vec (lookup service-name version))}))

     ;; Prefixed route params to separate easily from proxied params
     (ANY "/proxy/:_service-name/:_version/*" req
       (let [{service-name :_service-name, version :_version} (:route-params req)
             endpoints (lookup service-name version)
             ;; TODO: should have a more advanced strategy for choosing the
             ;;       endpoint eg. round robin, sticky shuffle, or even random
             endpoint (last endpoints)
             {:keys [host port]} endpoint]
         (proxy/proxy-request req host port))))))

(defn create-handler [registry]
  (-> (create-routes registry)
      api
      wrap-edn-params))

;; reload trick
;; (bss.resolve.core/run)
