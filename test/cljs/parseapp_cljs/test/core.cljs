(ns parseapp-cljs.test.core
  (:require-macros [parseapp-cljs.parse-macros :refer [defdrpc]]
                   [parseapp-cljs.async-macros :refer [<? go-catch]]
                   [cljs.core.async.macros :refer [go alt!]])
  (:require [parseapp-cljs.test.parse :as parse]))

(defdrpc runTests [request response]
  (.log js/console "running tests!")
  (go
   (try
     (<? (parse/run-tests))
     (.success response "tests ran successfully!")
     (catch :default e
       (.error js/console (aget e "stack"))
       (.error response (aget e "message"))))))
