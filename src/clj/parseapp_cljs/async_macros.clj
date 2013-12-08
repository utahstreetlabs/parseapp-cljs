(ns parseapp-cljs.async-macros
  (:require [cljs.core.async.macros :refer [go]]))

;;; thanks to David Nolen for this gem

(defmacro <? [expr]
  `(parseapp-cljs.async/throw-err (cljs.core.async/<! ~expr)))

(defmacro go-catch [& body]
  `(cljs.core.async.macros/go
    (try
      ~@body
      (catch :default e#
        e#))))
