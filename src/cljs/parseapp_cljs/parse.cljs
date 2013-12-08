(ns parseapp-cljs.parse
  (:require [cljs.core.async :as async :refer [chan close! put!]]
            [parseapp-cljs.core])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [parseapp-cljs.parse-macros :as parse :refer [go-try-catch]]
                   [parseapp-cljs.async-macros :refer [<?]])
  (:refer-clojure :exclude [find count]))

(def File (.-File js/Parse))
(def GeoPoint (.-GeoPoint js/Parse))
(def ParseObject (.-Object js/Parse))
(def ParseError (.-Error js/Parse))
(def Query (.-Query js/Parse))
(def User (.-User js/Parse))

(extend-type ParseObject
  ILookup
  (-lookup [obj key] (.get obj (name key)))

  IEquiv
  (-equiv [o other] (and (instance? (type o) other) (= (.-id o) (.-id other))))

  IHash
  (-hash [o] (hash (str (.-className o)"-"(.-id o))))
)

(defn fix-arguments
  "Fix an arguments object, turning it into a normal javascript array"
  [args]
  (.call (.-slice (.-prototype js/Array)) args))

(defn persisted? [obj]
  (not (.isNew obj)))

(defn object-id [obj]
  (.-id obj))

(defn save [parse-object properties]
  (let [ch (chan 1)]
    (.save parse-object (clj->js properties)
           (clj->js {"success" (fn [saved-object]
                                 (put! ch saved-object)
                                 (close! ch))
                     "error" (fn [object error]
                               (put! ch error)
                               (close! ch))}))
    ch))

(defn fetch [parse-object]
  (let [ch (chan 1)]
    (.fetch parse-object
            (clj->js {"success" (fn [fetched-object]
                                  (put! ch fetched-object)
                                  (close! ch))
                      "error" (fn [object error]
                                (put! ch error)
                                (close! ch))}))
    ch))

(defn log-in [email password]
  (let [ch (chan 1)]
    (.logIn User email password (clj->js {"success" (fn [user]
                                                      (put! ch user)
                                                      (close! ch))
                                          "error" (fn [user error]
                                                    (put! ch error)
                                                    (close! ch))}))
    ch))

(defn sign-up [name email password]
  (let [ch (chan 1)]
    (.signUp User email password (clj->js {:email email :displayName name})
             (clj->js {"success" (fn [user]
                                   (put! ch user)
                                   (close! ch))
                       "error" (fn [user error]
                                 (put! ch error)
                                 (close! ch))}))
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
                                 (put! ch error)
                                 (close! ch))}))
    ch))

(defn destroy [object]
  (let [ch (chan 1)]
    (.destroy object (clj->js {"success" (fn []
                                           (put! ch true)
                                           (close! ch))
                               "error" (fn [error]
                                         (put! ch error)
                                         (close! ch))}))
    ch))

(defn find [query]
  (let [ch (chan 1)]
    (.find query (clj->js {"success" (fn [objects]
                                       (put! ch (fix-arguments objects))
                                       (close! ch))
                           "error" (fn [error]
                                     (put! ch error)
                                     (close! ch))}))
    ch))

(defn count [query]
  (let [ch (chan 1)]
    (.count query (clj->js {"success" (fn [count]
                                        (put! ch count)
                                        (close! ch))
                            "error" (fn [error]
                                      (put! ch error)
                                      (close! ch))}))
    ch))

(defn count-all [cls]
  (count (Query. cls)))

(defn find-first [query]
  (go (first (<? (find query)))))

(defn find-user-by-email [email]
  (find-first (-> (Query. User) (.equalTo "email" email))))

(defn find-all [cls]
  (find (Query. cls)))

(defn find-all-users []
  (find-all User))

(defn find-or-create-user [email name password]
  (let [ch (chan 1)]
    (go
     (try
       (let [existing-user (<? (find-user-by-email email))]
         (if existing-user
           (>! ch existing-user)
           (>! ch (<? (save (User.) {"username" email "email" email "password" password "displayName" name}))))
         (close! ch))
       (catch :default e
         (>! ch e)
         (close! ch))))
    ch))

(defn destroy-all [objects]
  (let [ch (chan 1)]
    (.destroyAll ParseObject objects (clj->js {"success" (fn []
                                                           (put! ch true)
                                                           (close! ch))
                                               "error" (fn [error]
                                                         (put! ch error)
                                                         (close! ch))}))
    ch))
