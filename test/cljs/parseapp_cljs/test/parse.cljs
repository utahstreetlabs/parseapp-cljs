(ns parseapp-cljs.test.parse
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [parseapp-cljs.test.test-helpers :as h :refer [is= is deftest testing runner throws?]]
                   [parseapp-cljs.parse-macros :refer [defparsetype]]
                   [parseapp-cljs.async-macros :refer [<? go-catch]])
  (:require [cljs.core.async :as async :refer [chan]]
            [parseapp-cljs.parse :as parse :refer [save]]))

(defparsetype Widget)

(defn run-tests []
  (go-catch
   (try
    ;; the widget should be saved and it should get an id
    (is (not (nil? (aget (<? (save (Widget.) {:name "Chris"})) "id"))))

    ;; this should fail because name is a string column
    (is (throws? (<? (save (Widget.) {:name 1}))))

    (finally
      (<? (parse/destroy-all (<! (parse/find-all Widget))))))))
