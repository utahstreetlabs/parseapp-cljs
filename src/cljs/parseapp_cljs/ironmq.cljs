(ns parseapp-cljs.ironmq
  (:require [cemerick.url :refer [url URL]]
            [cljs.core.async :refer [timeout]]
            [parseapp-cljs.async :refer [prom->chan]]
            [parseapp-cljs.parse :refer [fix-arguments]])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [parseapp-cljs.parse-macros :refer [go-try-catch]]
                   [parseapp-cljs.async-macros :refer [<?]]))

;; pretty much stolen from:
;; https://github.com/iron-io/iron_mq_clojure/blob/master/src/iron_mq_clojure/client.clj
;; and modified to work in cljs w/ core.async and parse
;; dreams of generalizing and pushing back upstream currently on hold

(def aws-host "mq-aws-us-east-1.iron.io")
(def rackspace-host "mq-rackspace-dfw.iron.io")

(defn create-client
  "Creates an IronMQ client from the passed token, project-id and options.

  token - can be obtained from hud.iron.io/tokens
  project-id - can be obtained from hud.iron.io

  Options can be:
  :api-version - the version of the API to use, as an int. Defaults to 1.
  :scheme - the HTTP scheme to use when communicating with the server. Defaults to https.
  :host - the API's host. Defaults to aws-host, the IronMQ AWS cloud. Can be a string
          or rackspace-host, which holds the host for the IronMQ Rackspace cloud.
  :port - the port, as an int, that the server is listening on. Defaults to 443.
  :max-retries - maximum number of retries on HTTP error 503."
  [token project-id & options]
  (let [default {:token       token
                 :project-id  project-id
                 :api-version 1
                 :scheme      "https"
                 :host        aws-host
                 :port        443
                 :max-retries 5}]
    (merge default (apply hash-map options))))

(defn create-message
  "Creates a message that can then be pushed to a queue.
  body - is the body of the message.

  Options can be:
  :timeout - the timeout (in seconds) after which the message will be returned to the
             queue, after a successful get-message or get-messages. Defaults to 60.
  :delay - the delay (in seconds) before which the message will not be available on the
           queue after being pushed. Defaults to 0.
  :expires_in - the number of seconds to keep the message on the queue before deleting
                it automatically. Defaults to 604,800 (7 days). Max is 2,592,000 seconds
                (30 days)."
  [body & options]
  (merge {:body body} (apply hash-map options)))

(defn request
  "Sends an HTTP request to the IronMQ API.

  client - an instance of an IronMQ client, created with create-client.
  method - a string specifying the HTTP request method (GET, POST, etc.)
  endpoint - the IronMQ API endpoint following the project ID, with a leading /
  body - a string you would like to pass with the request. Set it to nil if not passing a body."
  [client method endpoint body]
  (let [path (str "/"
                  (:api-version client)
                  "/projects/"
                  (:project-id client)
                  endpoint)
        url (URL. (:scheme client)
                  nil
                  nil
                  (:host client)
                  (:port client)
                  path
                  nil
                  nil)
        url-str (str url)
        options {:method method
                 :headers {"Content-Type" "application/json"
                           "Accept" "application/json"
                           "Authorization" (str "OAuth " (:token client))}
                 :url (str url)
                 :body body}]
    (go-try-catch
      (loop [try 0]
        (let [response (<? (prom->chan (.httpRequest (.-Cloud js/Parse) (clj->js options))))
              status (.-status response)]
          (if (= status 200)
            (js->clj (.-data response) :keywordize-keys true)
            (js/Error. (str status))))))))

(defn queues
  "Returns a list of queues that a client has access to.

  client - an IronMQ client created with create-client."
  [client]
  (go-try-catch
    (map :name (<? (request client "GET" "/queues" nil)))))

(defn queue-size
  "Returns the size of a queue, as an int.

  client - an IronMQ client created with create-client.
  queue - the name of a queue, passed as a string."
  [client queue]
  (go-try-catch
    (:size (<? (request client "GET" (str "/queues/" queue) nil)))))

(defn post-messages
  "Pushes multiple messages to a queue in a single HTTP request.

  client - an IronMQ client, created with create-client.
  queue - the name of a queue, passed as a string.
  messages - an array of messages created with create-message. It can also be an
             array of strings, which will be used as the body and passed through
             create-message."
  [client queue & messages]
  (go-try-catch
    (fix-arguments
      (:ids (<? (request client "POST" (str "/queues/" queue "/messages")
                        (clj->js {:messages (map
                                             (fn [m]
                                               (if (string? m)
                                                 (create-message m) m))
                                                 messages)})))))))

(defn post-message
  "Pushes a single message to a queue.

  client - an IronMQ client, created with create-client.
  queue - the name of a queue, passed as a string.
  message - a message created with create-message. It can also be a string, which
            will be used as the body and passed through create-message."
  [client queue message]
  (go-try-catch
    (first (<? (post-messages client queue message)))))

(defn get-messages
  "Returns an array of messages that are on a queue.

  client - an IronMQ client, created with create-client.
  queue - the name of a queue, passed as a string.
  n - the number of messages to retrieve, passed as an int."
  [client queue n]
  (go-try-catch
    (fix-arguments
      (:messages (<? (request client "GET" (str "/queues/" queue "/messages?n=" n) nil))))))

(defn get-message
  "Returns a single message from a queue.

  client - an IronMQ client, created with create-client.
  queue - the name of a queue, passed as a string."
  [client queue]
  (go-try-catch
    (first (<? (get-messages client queue 1)))))

(defn delete-message
  "Removes a message from a queue.

  client - an IronMQ client, created with create-client.
  queue - the name of a queue, passed as a string.
  message - the message object to be removed, as retrieve from get-message
            or get-messages."
  [client queue message]
  (go-try-catch
    (<? (request client "DELETE" (str "/queues/" queue "/messages/" (get message "id")) nil))))
