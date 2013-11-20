(ns parseapp-cljs.express-macros)

(defmacro defexpress [config & body]
  `(-> (express/new-express-app ~config)
       ~@body
       .listen))
