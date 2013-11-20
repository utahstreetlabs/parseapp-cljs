(ns parseapp-cljs.core)

;; js* is evil of course, but I can't find another way to create a global var
;; since "window" isn't available
(js* "var setImmediate = function(fn) { fn(); }")

;; it's nice to be able to use prn
(set! *print-fn* (fn [x] (.log js/console x)))
