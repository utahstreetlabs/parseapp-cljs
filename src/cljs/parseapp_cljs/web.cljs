(ns parseapp-cljs.web)

(defn static [app tag]
  (.get app (str "/" tag)
        (fn [req res] (.render res tag))))

(defn render
  ([response template] (render response template {}))
  ([response template data] (.render response template (clj->js data))))
