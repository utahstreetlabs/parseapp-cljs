(ns parseapp-cljs.test.parse
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [parseapp-cljs.test.test-helpers :as h :refer [is= is deftest testing runner throws?]]
                   [parseapp-cljs.parse-macros :refer [defparsetype]]
                   [parseapp-cljs.async-macros :refer [<? go-catch]])
  (:require [cljs.core.async :as async :refer [chan]]
            [parseapp-cljs.parse :as parse :refer [save]]
            [schema.core :as s]))

(def WidgetValidator {(s/required-key :name) s/String
                      s/Keyword s/Any})
(defparsetype Widget {} {:schema WidgetValidator})

(defn map->Widget [map]
  (let [widget (Widget.)]
    (doseq [[key value] map]
      (.set widget (name key) value))
    widget))


(defn run-tests []
  (go-catch
   (try
     (is= 0 (<? (parse/count-all Widget)))

     (is (not (parse/persisted? (Widget.))))

     ;; the widget should be saved and it should get an id
     (let [bob-ch (save (Widget.) {:name "Bob" :type "meaty"})
           tom-ch (save (Widget.) {:name "Tom" :type "meaty"})
           cal-ch (save (Widget.) {:name "Cal" :type "veggie"})
           complex-ch (save (Widget.) {:name "Complex" :type "veggie"
                                       :data (clj->js {:foo "bar" :buz ["baz"]})
                                       :stuff (clj->js [1 2 3])})
           bob (<? bob-ch)
           tom (<? tom-ch)
           cal (<? cal-ch)
           complex (<? complex-ch)]

       (is (parse/persisted? bob))

       ;; make sure our implementation of ILookup works
       (is= "Bob" (:name bob))

       ;; this should throw because name is undefined and the validator forbids that
       (is (throws? (<? (save (Widget.) {:name nil}))))

       ;; this should throw because name is a string column
       (is (throws? (<? (save (Widget.) {:name 808}))))

       (is= 4 (<? (parse/count-all Widget)))

       (is= bob (<? (parse/find-first (-> (parse/Query. Widget) (.equalTo "name" "Bob")))))

       (is= bob (<? (parse/get-by-id Widget (parse/object-id bob))))

       (is= 2 (<? (parse/count (-> (parse/Query. Widget) (.equalTo "type" "meaty")))))
       (is= #{bob tom} (set (<? (parse/find (-> (parse/Query. Widget) (.equalTo "type" "meaty"))))))

       (is (<? (parse/destroy tom)))
       (is= 3 (<? (parse/count-all Widget)))

       (let [[jim rob] (<? (parse/save-all (map map->Widget [{:name "Jim"} {:name "Rob"}])))]
         (is (parse/persisted? jim))
         (is (parse/persisted? rob))
         (is= 5 (<? (parse/count-all Widget))))

       ;; make sure js->clj works with the odd objects returned by
       ;; parse Object fields this behavior comes from our extension
       ;; of IEncodeClojure to default, a liberty we take because we
       ;; are gods of this domain. or kings at least. or like, for
       ;; now, the only people here.
       (is= {:foo "bar", :buz ["baz"]}
            (js->clj (:data (<? (parse/find-first (-> (parse/Query. Widget) (.equalTo "name" "Complex")))))
                     :keywordize-keys true)))

    (finally
      (<? (parse/destroy-all (<! (parse/find-all Widget))))))))
