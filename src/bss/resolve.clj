(ns bss.resolve
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [bss.resolve.core :as app]))

(defn -main [& args]
  (let [[host port] args]
    (component/start
      (app/system {:host host :port port}))))
