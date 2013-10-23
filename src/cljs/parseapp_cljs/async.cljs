(ns parseapp-cljs.async
  (:require [cljs.core.async :as async :refer [chan close! put!]]))

;;; thanks to David Nolen for these gems

(defn error? [x]
  (or (instance? (.-Error js/Parse) x) (instance? js/Error x)))

(defn throw-err [x]
  (if (error? x)
    (throw x)
    x))

(defn put-result-callback [channel]
  (fn [object]
    (when object (put! channel object))
    (close! channel)))

(defn prom->chan [promise]
  (let [ch (chan 1)]
    (.then promise (put-result-callback ch) (put-result-callback ch))
    ch))

(defn fetch
  "returns a channel that will be given the result of fetching this object"
  [unfetched-object]
  (prom->chan (.fetch unfetched-object)))

(defn save [object properties]
  (prom->chan (.save object (clj->js properties))))
