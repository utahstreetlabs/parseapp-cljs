(ns parseapp-cljs.core)

;; it's nice to be able to use prn
(set! *print-fn* (fn [x] (.log js/console x)))
