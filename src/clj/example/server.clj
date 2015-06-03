(ns example.server
  (:require [clojure.set :refer [rename-keys]]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [aws.sdk.s3 :as s3]
            [environ.core :refer [env]]
            [ring.middleware.params :refer [wrap-params]]
            [s3-beam.handler :as s3b]))

;; Define the following enviroment variables where environ can find
;; them (i.e. /etc/profile/):
;;  - AWS_ZONE        i.e. eu-west-1
;;  - S3_BUCKET       i.e. my-bucket-name 
;;  - AWS_ACCESS_KEY  i.e. AKIAI7WJ6AVM37FHBOQZ
;;  - AWS_SECRET_KEY  i.e. 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

(def aws-config
  (assoc (select-keys env [:aws-zone :aws-access-key :aws-secret-key])
    :bucket "s3-beam-test" 
    :aws-zone "s3-eu-west-1")) ;; remove on next reset

(defn signed-url [aws-config key file-name]
  {:pre [(string? key) (string? file-name)]}
  (let [cred {:access-key (:aws-access-key aws-config) 
              :secret-key (:aws-secret-key aws-config)
              :endpoint (str (:aws-zone aws-config) ".amazonaws.com")}]
    (s3/generate-presigned-url cred (:bucket aws-config) key
      {:content-disposition (str "attachment; filename=" file-name)})))

(def cred {:access-key (:aws-access-key aws-config) 
           :secret-key (:aws-secret-key aws-config)
           :endpoint (str (:aws-zone aws-config) ".amazonaws.com")})

(defn uuid []
  (str (java.util.UUID/randomUUID)))

(defroutes routes
  (resources "/")
  (GET "/sign-download/:k/:f" [k f] 
    (try
      (println k)
      {:status 200
       :body (signed-url aws-config k f)}
      (catch Exception e
        (println e)
        {:status 500
         :body (pr-str e)})))
  (GET "/sign" {params :query-params}
    {:status 200
     :body (pr-str (s3b/sign-upload
                     (rename-keys params {"file-name" :file-name
                                          "mime-type" :mime-type})
                     aws-config
                     {:key-fn (fn [_] (uuid))}))}))

(def handler (wrap-params routes))

(defn fake-request [uri]
  (let [localhost "127.0.0.1"]
    (handler {:server-port 80
              :server-name localhost
              :remote-addr localhost
              :uri uri
              :scheme :http
              :headers {} 
              :request-method :get})))
