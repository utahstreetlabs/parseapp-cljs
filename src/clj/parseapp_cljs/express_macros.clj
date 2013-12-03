(ns parseapp-cljs.express-macros)

(defmacro defexpress [config & body]
  `(-> (parseapp-cljs.express/new-express-app ~config)
       ~@body
       .listen))
