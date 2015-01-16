(ns bss.resolve.core
  (:require [com.stuartsierra.component :as component]
            [bss.resolve.web :as web]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]))

(defonce system (atom nil))

(defrecord Webserver [port server]
  component/Lifecycle

  (start [component]
    (print "Starting web server on port" port ".\n")
    (let [server (run-jetty #'web/http-handler
                            {:port port, :join? false})]
      (assoc component :server server)))

  (stop [component]
    (print "Stopping web server on port" port ".\n")
    (.stop server)
    (assoc component :server nil)))

(defn new-webserver [port]
  (map->Webserver {:port (Integer. (or port (env :port) 10555))}))

(defn resolve-system [config-options]
  (let [{:keys [port]} config-options]
    (component/system-map
     :web (new-webserver port))))


(defn run [& [port]]
  (if @system (swap! system component/stop))
  (reset! system (resolve-system {:port port}))
  (swap! system component/start))

(defn -main [& [port]]
  (run port))

(comment
  (run))
