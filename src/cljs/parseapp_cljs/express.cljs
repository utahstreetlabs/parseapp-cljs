(ns parseapp-cljs.express
  (:require [parseapp-cljs.parse :refer [User]]))

(def express (js/require "express"))
(def parse-express-raw-body (js/require "parse-express-raw-body"))
(def parse-express-cookie-session (js/require "parse-express-cookie-session"))

(defn static [app tag]
  (.get app (str "/" tag)
        (fn [req res] (.render res tag))))

(defn new-express-app [config]
  (let [app (express)
        cookie-secret (:cookie-secret config)
        cookie-max-age (or (:cookie-max-age config) 3600000)]
    (.set app "views" (get config :views-dir "cloud/views"))
    (.set app "view engine" (get config :view-engine "jade"))
    (when (:body-parser config) (.use app (.bodyParser express)))
    (when (:parse-raw-body config) (.use app (parse-express-raw-body)))
    (when cookie-secret
      (.use app (.cookieParser express cookie-secret))
      (.use app (.cookieSession express {:cookie {:maxAge cookie-max-age}}))
      (.use app (parse-express-cookie-session (clj->js {:cookie {:maxAge cookie-max-age}}))))

    (when (:uses config)
      (doseq [use (:uses config)] (.use app use)))
    (when (:initializer config) ((:initializer config) app))

    (.use app (.-router app))
    (doseq [tag (:static config)] (static app tag))
    (when (:locals config) (.locals app (clj->js (:locals config))))

    app))

(defn render
  ([response template] (render response template {}))
  ([response template data] (.render response template (clj->js data))))

(defn render-json
  ([response] (render-json response 200 {}))
  ([response data] (render-json response data {}))
  ([response status data] (.json response status (clj->js data))))

(defn redirect-after-post [response url]
  (.redirect response 303 url))

(defn current-user []
  (.current User))

(defn logged-in? []
  (boolean (current-user)))

(defn force-login
  ([handler url-getter]
    (fn [request response]
      (if (logged-in?)
        (handler request response)
        (if (.-xhr request)
          (render-json response 400 {:error "You must be logged in to perform this action."})
          (redirect-after-post response (url-getter request))))))
  ([handler] (force-login handler (constantly "/"))))
