(ns parseapp-cljs.async
  (:require [cljs.core.async :as async :refer [chan close! put!]]))

;;; thanks to David Nolen for these gems

(defn error? [x]
  (or (instance? (.-Error js/Parse) x) (instance? js/Error x)))

(defn throw-err [x]
  (cond
   (instance? js/Error x)
   (throw x)

   (instance? (.-Error js/Parse) x)
   (throw (js/Error. (str "Parse Error "(.-code x)": "(.-message x))))

   :else x))

(defn put-result-callback [channel]
  (fn [object]
    (when-not (nil? object) (put! channel object))
    (close! channel)))

(defn prom->chan [promise]
  (let [ch (chan 1)]
    (.then promise (put-result-callback ch) (put-result-callback ch))
    ch))
