(ns bss.resolve
  (:gen-class)
  (:require [bss.resolve.core :as app]
            [com.stuartsierra.component :as component]))

(defn -main [& args]
  (let [[host port] args]
    (component/start
      (app/system {:host host :port port}))))
