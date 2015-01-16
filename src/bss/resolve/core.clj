(ns bss.resolve.core
  (:require [com.stuartsierra.component :as component]
            [bss.resolve.web :as web]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]))

(comment
  (defrecord Database [host port connection]
    component/Lifecycle

    (start [component]
      (println ";; Starting database")
      (let [conn (connect-to-database host port)]
        (assoc component :connection conn)))

    (stop [component]
      (println ";; Stopping database")
      (.close connection)
      ;; watch out for dissoc on base field, will lose protocol
      (assoc component :connection nil)))

  (defn new-database [host port]
    (map->Database {:host host :port port}))

  (defn new-scheduler [& _])

  (defrecord ExampleComponent [options cache database scheduler]
    component/Lifecycle

    (start [this]
      (println ";; Starting ExampleComponent")
      (assoc this :admin (get-user database "admin")))

    (stop [this]
      (println ";; Stopping ExampleComponent")
      this))

  (defn example-component [config-options]
    (map->ExampleComponent {:options config-options
                            :cache (atom {})}))

  (defn system [config-options]
    (let [{:keys [host port]} config-options]
      (component/system-map
       :db (new-database host port)
       :scheduler (new-scheduler)
       :app (component/using
             (example-component config-options)
             {:database  :db
              :scheduler :scheduler})))))

;;; --------------------


;; TODO: replace with system components

(defonce server (atom nil))

(defn run [& [port]]
  (if @server
    (.stop @server)
    (reset! server nil))
  (let [port (Integer. (or port (env :port) 10555))]
    (print "Starting web server on port" port ".\n")
    (reset! server
            (run-jetty #'web/http-handler {:port port
                                           :join? false})))
  server)

(defn -main [& [port]]
  (run port))

(comment
  (run))
