(ns parseapp-cljs.core)

;; it's nice to be able to use prn
(set! *print-fn* (.-log js/console))
(set! *main-cli-fn* #())
