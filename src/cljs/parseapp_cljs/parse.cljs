(ns parseapp-cljs.parse)

(js* "var setImmediate = function(fn) { fn(); }")
(set! *print-fn* (fn [x] (.log js/console x)))

(def Query (.-Query js/Parse))
(def ParseError (.-Error js/Parse))
(def GeoPoint (.-GeoPoint js/Parse))
(def File (.-File js/Parse))
(def User (.-User js/Parse))

