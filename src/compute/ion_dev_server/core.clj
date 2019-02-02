(ns compute.ion-dev-server.core
  (:require
    [com.stuartsierra.component :as component]
    [ring.middleware.reload :as reload]
    [ring.middleware.cors :as cors]
    [org.httpkit.server :as server]))

(def the-handler)

(def handler (-> #'the-handler
                 (cors/wrap-cors :access-control-allow-origin (constantly true)
                                 :access-control-allow-methods [:post :options]
                                 :access-control-allow-headers ["X-CSRF-Token" "Set-Cookie" "Content-Type"]
                                 :access-control-allow-credentials "true")))

(def handler-with-reload (reload/wrap-reload #'handler {:dirs ["src" "siderail"]}))

(defn start
  [handler]
  (alter-var-root #'the-handler (constantly handler))
  (server/run-server #'handler-with-reload
                     {:port 8880}))

(defn stop
  [server]
  (server))

(defrecord HttpServer [handler]
  component/Lifecycle
  (start [component]
    (assoc component :http-server (start handler)))
  (stop [component]
    (when-let [s (:http-server component)]
      (stop s))
    (dissoc component :http-server)))

(defn new-http-server
  [{:keys [handler]}]
  (map->HttpServer {:handler handler}))