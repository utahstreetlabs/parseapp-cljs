(ns parseapp-cljs.parse
  (:require [cljs.core.async :as async :refer [chan close! put!]]
            [parseapp-cljs.core]
            [schema.core :as schema])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [parseapp-cljs.parse-macros :as parse]
                   [parseapp-cljs.async-macros :refer [<? go-catch]])
  (:refer-clojure :exclude [find count]))

(def File (.-File js/Parse))
(def GeoPoint (.-GeoPoint js/Parse))
(def ParseObject (.-Object js/Parse))
(def ParseError (.-Error js/Parse))
(def Query (.-Query js/Parse))
(def User (.-User js/Parse))
(def Push (.-Push js/Parse))
(def Installation (.-Installation js/Parse))

(defn coerce-to-date [thing]
  (when thing (js/Date. thing)))

(def time-regex (js/RegExp. "At$"))

(defn coerce-on-key [object key]
  (if (.match key time-regex)
    (js/Date. object)
    object))

(extend-type ParseObject
  ILookup
  (-lookup [obj key]
    (case key
      :id (.-id obj)
      ;; wrap dates so they're easier to serialize
      :createdAt (coerce-to-date (.-createdAt obj))
      :updatedAt (coerce-to-date (.-updatedAt obj))
       (.get obj (name key))))

  IAssociative
  (-contains-key? [o k] (.has o (name k)))
  (-assoc [o k v]
    (let [new-obj (.clone o)]
      (.set new-obj (name k) v)
      new-obj))

  IEquiv
  (-equiv [o other] (and (instance? (type o) other) (= (.-id o) (.-id other))))

  IHash
  (-hash [o] (hash (str (.-className o)"-"(.-id o))))

  IEncodeClojure
  (-js->clj [parse-object opts]
    (-> (reduce (fn [m key] (assoc m
                              (keyword key)
                              (-> (.get parse-object key)
                                  (coerce-on-key key)
                                  (js->clj :keywordize-keys true))))
                {}
                (.keys js/Object (.toJSON parse-object)))
        (assoc :id (.-id parse-object))
        (assoc :createdAt (coerce-to-date (.-createdAt parse-object)))
        (assoc :updatedAt (coerce-to-date (.-updatedAt parse-object)))))

  IPrintWithWriter
  (-pr-writer [parse-object writer opts]
    (-pr-writer (-js->clj parse-object {:keywordize-keys true}) writer opts)))

(def v8-object-type (type (.toJSON (ParseObject.))))

(extend-type default
  IEncodeClojure
  (-js->clj [object {:keys [keywordize-keys] :as opts}]
    (let [{:keys [keywordize-keys]} opts
            keyfn (if keywordize-keys keyword str)
            f (fn thisfn [x]
                (cond
                  (seq? x)
                  (doall (map thisfn x))

                  (coll? x)
                  (into (empty x) (map thisfn x))

                  (array? x)
                  (vec (map thisfn x))

                  (identical? (type x) js/Object)
                  (into {} (for [k (js-keys x)]
                             [(keyfn k) (thisfn (aget x k))]))

                  (identical? (type x) v8-object-type)
                  (into {} (for [k (.keys js/Object x)]
                             [(keyfn k) (thisfn (aget x k))]))

                  :else x))]
        (f object))))

(defn attrs->clj
  ([x] (js->clj x))
  ([x options] (apply js->clj x (flatten (vec options)))))

(defn use-master-key! []
  (.useMasterKey (.-Cloud js/Parse)))

(defn fix-arguments
  "Fix an arguments object, turning it into a normal javascript array"
  [args]
  (.call (.-slice (.-prototype js/Array)) args))

(defn persisted? [obj]
  (not (.isNew obj)))

(defn object-id [obj]
  (.-id obj))

(defn add-validator [methods options]
  (if (:schema options)
    [(assoc methods :validate
            (fn [attrs opts]
              (try
                (schema/validate (:schema options) (js->clj attrs :keywordize-keys true))
                false
                (catch :default e
                  e))))
     (dissoc options :schema)]
    [methods options]))

(defn extend-parse-object [subclass-name methods options]
  (let [[updated-methods updated-options] (add-validator methods options)]
    (.extend (.-Object js/Parse) subclass-name (clj->js updated-methods) (clj->js updated-options))))


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

(defn save-all
  [objects]
  (let [ch (chan 1)]
    (.saveAll ParseObject (clj->js objects)
              (clj->js {:success (fn [list b]
                                   (put! ch list)
                                   (close! ch))
                        :error (fn [e]
                                 (put! ch e)
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
  (go-catch (first (<? (find (.limit query 1))))))

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


;;;; push notifications ;;;;


(defn ios-installations-query []
  (doto (Query. Installation)
    (.equalTo "deviceType" "ios")))

(defn user-ios-installation-query [user]
  (doto (ios-installations-query)
    (.equalTo "user" user)))

(defn user-ios-installed? [user]
  (use-master-key!)
  (go-catch
   (< 0 (<? (count (user-ios-installation-query user))))))

(defn send-push-notification [query data]
  (let [ch (chan 1)]
    (.send Push
           (clj->js {:where query
                     :data data})
           (clj->js {:success (fn []
                                (put! ch true)
                                (close! ch))
                     :error (fn [error]
                              (put! ch error)
                              (close! ch))}))
    ch))
