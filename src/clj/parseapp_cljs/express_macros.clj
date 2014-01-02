(ns parseapp-cljs.express-macros)

(defmacro defexpress [config & body]
  `(-> (parseapp-cljs.express/new-express-app ~config)
       ~@body
       .listen))

(defmacro go-endpoint [[request response] & body]
  `(fn [~request ~response]
     (cljs.core.async.macros/go
       (try
         ~@body
         (catch :default e#
           (.error js/console (.-stack e#))
           (.send ~response 500 (.-message e#)))))))
