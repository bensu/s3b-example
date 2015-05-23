(defproject example "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3211"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/data.json "0.2.5"]
                 [sablono "0.3.4"]
                 [org.omcljs/om "0.8.8"]
                 [prismatic/om-tools "0.3.11" :exclusions [om]]]

  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-figwheel "0.3.1"]]

  :source-paths ["src" "../s3_beam/src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  
  :figwheel {:css-dirs ["resources/public/css"]}

  :cljsbuild
  {:builds [{:id "dev"
             :source-paths ["src" "../s3_beam/src/cljs"]

             :figwheel { :on-jsload "example.core/on-js-reload" }

             :compiler {:main example.core
                        :asset-path "js/compiled/out"
                        :output-to "resources/public/js/compiled/example.js"
                        :output-dir "resources/public/js/compiled/out"
                        :source-map-timestamp true }}
            {:id "min"
             :source-paths ["src"]
             :compiler {:output-to "resources/public/js/compiled/example.js"
                        :main example.core                         
                        :optimizations :advanced
                        :pretty-print false}}]})
