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
     (is= 0 (<? (parse/count-all Widget)))

     (is (not (parse/persisted? (Widget.))))

     ;; the widget should be saved and it should get an id
     (let [bob-ch (save (Widget.) {:name "Bob" :type "meaty"})
           tom-ch (save (Widget.) {:name "Tom" :type "meaty"})
           cal-ch (save (Widget.) {:name "Cal" :type "veggie"})
           bob (<? bob-ch)
           tom (<? tom-ch)
           cal (<? cal-ch)]

       (is (parse/persisted? bob))

       ;; make sure our implementation of ILookup works
       (is= "Bob" (:name bob))

       ;; this should throw because name is a string column
       (is (throws? (<? (save (Widget.) {:name 808}))))

       (is= 3 (<? (parse/count-all Widget)))

       (is= bob (<? (parse/find-first (-> (parse/Query. Widget) (.equalTo "name" "Bob")))))

       (is= bob (<? (parse/get-by-id Widget (parse/object-id bob))))

       (is= 2 (<? (parse/count (-> (parse/Query. Widget) (.equalTo "type" "meaty")))))
       (is= #{bob tom} (set (<? (parse/find (-> (parse/Query. Widget) (.equalTo "type" "meaty"))))))

       (is (<? (parse/destroy tom)))
       (is= 2 (<? (parse/count-all Widget))))

    (finally
      (<? (parse/destroy-all (<! (parse/find-all Widget))))))))
