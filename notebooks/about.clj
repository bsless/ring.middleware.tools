{:nextjournal.clerk/visibility {:result :hide :code :hide}}
(ns about
  (:require
   [nextjournal.clerk :as clerk]
   [bsless.ring.middleware.tools :as mwt])
  (:import
   (java.util.function Function)
   (java.util.concurrent CompletableFuture)))

(comment
  (clerk/show! "notebooks/about.clj")
  (clerk/serve! {:watch-paths ["notebooks"] :browse? true}))

;;; # Ring Middleware Tools

;;; Writing middlewares sucks. There, I said it.

;;; It's not fun, covering both arities is boilerplate-y, it's easy to
;;; get wrong, and hard and confusing if you want to invoke non-blocking
;;; functions in your middlewares. Callback hell galore.

;;; This library isn't a silver bullet. It's aimed at the majority of use
;;; cases, mainly to save everyone a bit of headache

;;; Hopefully it can help turn something like
;;; [this](https://github.com/ring-clojure/ring/blob/master/ring-core/src/ring/middleware/head.clj):

{:nextjournal.clerk/visibility {:result :hide :code :fold}}

(defn head-request
  "Turns a HEAD request into a GET."
  {:added "1.2"}
  [request]
  (if (= :head (:request-method request))
    (assoc request :request-method :get)
    request))

(defn head-response
  "Returns a nil body if original request was a HEAD."
  {:added "1.2"}
  [response request]
  (if (and response (= :head (:request-method request)))
    (assoc response :body nil)
    response))

{:nextjournal.clerk/visibility {:result :hide :code :show}}

(defn wrap-head
  "Middleware that turns any HEAD request into a GET, and then sets the response
  body to nil."
  {:added "1.1"}
  [handler]
  (fn
    ([request]
     (-> request
         head-request
         handler
         (head-response request)))
    ([request respond raise]
     (handler (head-request request)
              (fn [response] (respond (head-response response request)))
              raise))))

;;; Into this:

(defn wrap-head'
  [handler]
  (-> handler (mwt/before head-request) (mwt/around identity head-response)))

;;; Certainly looks better

;;; Does it work?

;;; Given this request

(def request
  {:request-method :head :body "hello" :other "thing"})

;;; And this expected result

{:nextjournal.clerk/visibility {:result :show :code :show}}

(def expect
  ((wrap-head identity) request))

;;; Both arities of our middleware work

(= ((wrap-head identity) request)
   ((wrap-head (fn [x r _] (r x))) request identity identity)

   ((wrap-head' identity) request)
   ((wrap-head' (fn [x r _] (r x))) request identity identity))


;;; ## Building Blocks

;;; Middlewares, generally, can do something with a request, a response,
;;; or both.

;;; This maps neatly to _before_, _after_, and _around_.

;;; ### Before

;;; You've already seen an example of it

(fn [handler] (-> handler (mwt/before head-request)))

;;; Simply put, it intercepts the request before passing it to the handler

;;; ### After

;;; Just like `before` (ha ha), intercepts the response from the handler

(defn wrap-with-inc-count
  "After the handler, inc the :count in the response"
  [handler]
  (-> handler (mwt/after (fn [response] (update response :count inc)))))

(let [handler (fn [request] (update request :count (partial * 2)))
      wrapped (wrap-with-inc-count handler)]
  (wrapped {:count 3}))

;;; ### Around

;;; Sometimes, you need to pass around context for when you handle the response.

;;; Besides the handler, `around` takes two functions, one to create the
;;; initial context and the other to combine it with the subsequent
;;; result

(fn [handler]
  (-> handler (mwt/around identity head-response)))

;;; Another example, measuring the handling time of a request:

(fn [handler]
  (-> handler
      (mwt/around-long
       (fn ^long [_] (System/currentTimeMillis))
       (fn [response ^long ctx]
         (println (- (System/currentTimeMillis) ctx))
         response))))

;;; This one uses `around-long` to efficiently carry around a primitive
;;; context. Why not?

;;; ### About async

;;; Both `before` and `after` have async versions, `before-async` and
;;; `after-async`.

;;; In the non-blocking case they're invoked with success and failure
;;; continuations, for example:

(defn spam-async
  ([x] (future {:spam x}))
  ([x sk _fk]
   (.thenApplyAsync
    (CompletableFuture/completedFuture
     {:spam x})
    (reify Function
      (apply [_ ret] (sk ret))))))

;;; Then used to wrap like so:

(fn [handler] (mwt/before-async handler spam-async))

;;; You'll have to close-over any thread pool you want to pass around, apologies.

;;; ### Convenience

;;; Sometimes we don't care about the results, but just want side
;;; effects. For this purpose use `effect-before` and `effect-after`
;;; which discard the result of the effectful function. Useful for
;;; logging and tracing

((mwt/effect-before identity println) {:a 1})

;;; or

(let [a (atom nil)
      handler (fn [x] (update x :a inc))
      effect (fn [x] (reset! a x))
      in {:a 1}]
  {:result ((mwt/effect-before handler effect) in)
   :atom @a})

;;; These also come with asynchronous versions.
