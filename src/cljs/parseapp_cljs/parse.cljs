(ns parseapp-cljs.parse
  (:require [cljs.core.async :as async :refer [chan close! put!]]
            [parseapp-cljs.core])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [parseapp-cljs.parse-macros :as parse :refer [go-try-catch]]
                   [parseapp-cljs.async-macros :refer [<?]])
  (:refer-clojure :exclude [find]))

(def File (.-File js/Parse))
(def GeoPoint (.-GeoPoint js/Parse))
(def Object (.-Object js/Parse))
(def ParseError (.-Error js/Parse))
(def Query (.-Query js/Parse))
(def User (.-User js/Parse))

(defn- fix-arguments
  "Fix an arguments object, turning it into a normal javascript array"
  [args]
  (.call (.-slice (.-prototype js/Array)) args))

(defn save [parse-object properties]
  (let [ch (chan 1)]
    (.save parse-object (clj->js properties)
           (clj->js {"success" (fn [saved-object] (put! ch saved-object))
                     "error" (fn [object error] (put! ch error))}))
    ch))

(defn fetch [parse-object]
  (let [ch (chan 1)]
    (.fetch parse-object
            (clj->js {"success" (fn [fetched-object] (put! ch fetched-object))
                      "error" (fn [error] (put! ch error))}))
    ch))

(defn find-all [cls]
  (let [ch (chan 1)]
    (-> (Query. cls)
      (.find (clj->js {"success" (fn [objects]
                                   (when objects
                                     (put! ch (fix-arguments objects)))
                                   (close! ch))
                       "error" (fn [error] (put! ch error))})))
    ch))

(defn find-all-users []
  (find-all User))

(defn find-user-by-email [email]
  (let [ch (chan 1)]
    (-> (Query. User) (.equalTo "email" email)
        (.find (clj->js {"success" (fn [objects]
                                     (let [user (aget objects 0)]
                                       (when user (put! ch user))
                                       (close! ch)))
                         "error" (fn [error] (put! ch error))})))
    ch))

(defn find-or-create-user [email name password]
  (let [ch (chan 1)]
    (go
     (try
       (let [existing-user (<? (find-user-by-email email))]
         (if existing-user
           (>! ch existing-user)
           (>! ch (<? (save (User.) {"username" email "email" email "password" password "displayName" name})))))
       (catch js/Error e
         (>! ch e))
       (catch ParseError e
         (>! ch e))))
    ch))

(defn log-in [email password]
  (let [ch (chan 1)]
    (.logIn User email password (clj->js {"success" (fn [user] (put! ch user))
                                          "error" (fn [user error] (put! ch error))}))
    ch))

(defn sign-up [name email password]
  (let [ch (chan 1)]
    (.signUp User email password (clj->js {:email email :displayName name})
             (clj->js {"success" (fn [user] (put! ch user))
                       "error" (fn [user error] (put! ch error))}))
    ch))

(defn get-by-id [type id & [{:keys [includes] :or {includes []}}]]
  (let [ch (chan 1)
        query (Query. type)]
    (doseq [include includes]
      (.include query (clj->js include)))
    (.get query id (clj->js {"success" (fn [object]
                                         (when object (put! ch object))
                                         (close! ch))
                             "error" (fn [object error]
                                 (put! ch error))}))
    ch))

(defn find-first [query]
  (let [ch (chan 1)]
    (.find query (clj->js {"success" (fn [objects]
                                       (let [object (aget objects 0)]
                                         (when object (put! ch object))
                                         (close! ch)))
                           "error" (fn [error] (put! ch error))}))
    ch))

(defn find [query]
  (let [ch (chan 1)]
    (.find query (clj->js {"success" (fn [objects]
                                       (put! ch (fix-arguments objects)))
                           "error" (fn [error] (put! ch error))}))
    ch))

(defn destroy [object]
  (let [ch (chan 1)]
    (.destroy object (clj->js {"success" (fn [] (put! ch true))
                               "error" (fn [error] (put! ch error))}))
    ch))
