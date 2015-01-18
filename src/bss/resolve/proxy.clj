(ns bss.resolve.proxy
  (:require [bss.resolve.registry :as registry]
            [clj-http.client :refer [request]]
            [clojure.string :as str]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [tailrecursion.ring-proxy :refer [prepare-cookies slurp-binary]])
  (:import (java.net URI)))

(defn select-endpoint [req endpoints]
  ;; TODO: should have a more advanced strategy for choosing the
  ;;       endpoint eg. round robin, sticky shuffle, or even random
  (last endpoints))

(defn- calculate-remote-uri [registry req]
  (let [segments               (str/split (req :uri) #"\/")
        [service-name version] (drop 2 segments)
        path                   (str/join "/" (drop 4 segments))
        endpoints              (registry/lookup registry service-name version)
        {:keys [host port]}    (select-endpoint req endpoints)]
    ;; TODO: if opts besides :host and :port provided, mix into request?
    (if (and host port)
      (format "%s://%s:%s/%s" (name (:scheme req)) host port path))))

(defn- proxy-to [remote-uri-base req http-opts]
  (let [uri        (URI. remote-uri-base)
        remote-uri (URI. (.getScheme uri)
                         (.getAuthority uri)
                         (.getPath uri)
                         nil
                         nil)]
    (-> (merge {:method (:request-method req)
                :url (str remote-uri "?" (:query-string req))
                :headers (dissoc (:headers req) "host" "content-length")
                :body (if-let [len (get-in req [:headers "content-length"])]
                        (slurp-binary (:body req) (Integer/parseInt len)))
                :follow-redirects true
                :throw-exceptions false
                :as :stream} http-opts)
        request
        prepare-cookies)))

(defn wrap-dynamic-proxy
  "Proxies requests to proxied-path, a local URI, to the remote URI at
  remote-uri-base, also a string."
  [handler registry ^String proxied-path & [http-opts]]
  (wrap-cookies
   (fn [req]
     (if (.startsWith ^String (:uri req) (str proxied-path "/"))
       (let [remote-uri-base (calculate-remote-uri registry req)]
         (or (and remote-uri-base (proxy-to remote-uri-base req http-opts))
             {:status 402, :body "Service not resolved"}))
       (handler req)))))
