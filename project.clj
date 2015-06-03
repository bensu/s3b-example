(defproject sb3-example "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3211"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/data.json "0.2.5"]
                 [environ "1.0.0"]
                 [ring "1.3.2"]
                 [compojure "1.3.1"]
                 [sablono "0.3.4"]
                 [org.omcljs/om "0.8.8"]
                 [com.amazonaws/aws-java-sdk "1.7.5"]
                 [clj-time "0.6.0"]
                 [prismatic/om-tools "0.3.11" :exclusions [om]]]

  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-environ "1.0.0"]
            [lein-ring "0.9.4"]
            [lein-figwheel "0.3.1"]]

  :source-paths ["src/clj" "../clj_aws_s3/src/clj" "../s3_beam/src/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]}
  
  :ring {:handler example.server/handler}

  :cljsbuild
  {:builds [{:id "dev"
             :source-paths ["src/cljs" "../s3_beam/src/cljs"]

             :figwheel {:on-jsload "example.core/on-js-reload"}

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
