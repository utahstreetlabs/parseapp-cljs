(ns parseapp-cljs.parse-macros
  (:require [cljs.core.async.macros :refer [go]]))

(defmacro defdrpc [name args & body]
  `(.define (.-Cloud js/Parse) ~(str name) (fn ~args ~@body)))

(defmacro defjob [name args & body]
  `(.job (.-Cloud js/Parse) ~(str name) (fn ~args ~@body)))

(defmacro defparsetype [name & [methods options]]
  `(def ~name (.extend (.-Object js/Parse) ~(str name) ~methods ~options)))

(defmacro go-try-catch [& body]
  `(go
    (try
      ~@body
      (catch parseapp-cljs.parse/ParseError e#
        (throw (js/Error. (.-message e#))))
      (catch js/Error e#
        (throw (js/Error. (.-message e#)))))))
