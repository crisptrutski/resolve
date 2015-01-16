(defproject bss/resolve "0.1.0-SNAPSHOT"
  :description "Service registry and proxy"
  :url "https://github.com/organosoft/resolve"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.stuartsierra/component "0.2.2"]
                 [org.clojure/tools.namespace "0.2.8"]
                 [com.taoensso/carmine "2.9.0"]
                 [compojure "1.3.1"]
                 [bss/rampant "0.1.0-SNAPSHOT"]
                 [ring "1.3.2"]]

  :plugins [[lein-environ "1.0.0"]]

  :min-lein-version "2.5.0"

  :uberjar-name "resolve.jar"

  :profiles {:dev {:repl-options {:init-ns bss.resolve.core}
                   :env {:is-dev true}}}

             :prod {:jvm-opts ["-Xmx1g" "-server"] }

             :uberjar {:env {:production true}
                       :omit-source true
                       :aot :all})
