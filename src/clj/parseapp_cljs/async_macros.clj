(ns parseapp-cljs.async-macros)

;;; thanks to David Nolen for this gem

(defmacro <? [expr]
  `(parseapp-cljs.async/throw-err (cljs.core.async/<! ~expr)))
