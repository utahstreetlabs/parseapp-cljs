(ns parseapp-cljs.express)

(def express (js/require "express"))
(def parse-express-raw-body (js/require "parse-express-raw-body"))
(def parse-express-cookie-session (js/require "parse-express-cookie-session"))

(defn static [app tag]
  (.get app (str "/" tag)
        (fn [req res] (.render res tag))))

(defn new-express-app [config]
  (let [app (express)
        cookie-secret (:cookie-secret config)]
    (.set app "views" (get config :views-dir "cloud/views"))
    (.set app "view engine" (get config :view-engine "jade"))
    (when (:body-parser config) (.use app (.bodyParser express)))
    (when (:parse-raw-body config) (.use app (parse-express-raw-body)))
    (when cookie-secret
      (.use app (.cookieParser express cookie-secret))
      (.use app (parse-express-cookie-session (clj->js {:cookie {:maxAge 3600000}}))))
    (.use app (.-router app))
    (doseq [tag (:static config)] (static app tag))
    app))
