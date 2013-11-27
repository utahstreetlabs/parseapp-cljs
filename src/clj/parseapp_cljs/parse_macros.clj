(ns parseapp-cljs.parse-macros
  (:require [cljs.core.async.macros :refer [go]]))

(defmacro defdrpc [name args & body]
  `(.define (.-Cloud js/Parse) ~(str name) (fn ~args ~@body)))

(defmacro defjob [name args & body]
  `(.job (.-Cloud js/Parse) ~(str name) (fn ~args ~@body)))

(defmacro defhook [class hook-name args & body]
  `(. (.-Cloud js/Parse) ~hook-name ~class (fn ~args ~@body)))

(defmacro defparsetype [name & [methods options]]
  `(def ~name (.extend (.-Object js/Parse) ~(str name) ~methods ~options)))

(defmacro go-try-catch [& body]
  `(go
    (try
      ~@body
      (catch :default e#
        e#))))
