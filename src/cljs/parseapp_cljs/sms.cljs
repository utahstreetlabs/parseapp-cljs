(ns parseapp-cljs.sms
  (:require [cljs.core.async :as async :refer [chan close! put!]]))

(defn send [twilio from to body]
  (let [ch (chan 1)]
    (.sendSms twilio
              (clj->js {:from from :to to :body body})
              (fn [error response-data]
                (if error
                  (do
                    (put! ch (js/Error. (.-moreInfo error)))
                    (close! ch))
                  (do
                    (put! ch true)
                    (close! ch)))))
    ch))
