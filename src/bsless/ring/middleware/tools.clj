(ns bsless.ring.middleware.tools
  (:import
   (clojure.lang IFn$OL IFn$OLO)))

(set! *warn-on-reflection* true)

(defn before
  "Return a new handler which calls `f` on the request before passing it to `handler`.
  Optionally takes `comb`, (fn [request result]) -> result, to allow
  separation of `f`'s calculation from how its result should be passed
  to the handler in the new request.

  For example:
  ```clojure
  (before handler #(assoc % :id (get-id-from-db %)))
  ```
  and
  ```clojure
  (before handler get-id-from-db (fn [request id] (assoc request :id id)))
  ```"
  ([handler f]
   (fn
     ([request]
      (handler (f request)))
     ([request respond raise]
      (handler (f request) respond raise))))
  ([handler f comb]
   (before handler (fn [request] (comb request (f request))))))

(defn- discard-result [request _result] request)

(defn effect-before
  "Invoke an effectful function `f` before continuing with `handler`.
  Its result is discarded."
  [handler f]
  (before handler f discard-result))

(defn before-async
  "Invoke async function `af` before the handler, similarly to [[before]].
  `af` can have two arities which will be invoked depending on how the handler is called:
  - (fn af [x]) returns a deref-able result
  - (fn af [x on-success on-failure]) takes continuation callbacks.
  The handler is invoked in the continuation.
  Also takes (fn comb [request result]) -> request as an optional argument."
  ([handler af]
   (fn
     ([request]
      (handler @(af request)))
     ([request respond raise]
      (af request (fn sk [res] (handler res respond raise)) raise))))
  ([handler af comb]
   (fn
     ([request]
      (handler (comb request @(af request))))
     ([request respond raise]
      (af request (fn sk [res] (handler (comb request res) respond raise)) raise)))))

(defn effect-before-async
  "Invoke an asynchronous effect before the handler, similarly to [[effect-before]].
  The handler will be called only AFTER the effect completes."
  [handler af]
  (before-async handler af discard-result))

(defn after
  "Return a new handler which calls `f` on the response from `handler`.
  Optionally takes `comb`, (fn [request result]) -> result, to allow
  separation of `f`'s calculation from how its result should be passed
  to the handler in the new response.

  For example:
  ```clojure
  (after handler (fn [response] (doto response log)))
  ```"
  ([handler f]
   (fn
     ([request]
      (f (handler request)))
     ([request respond raise]
      (handler request (before respond f) raise))))
  ([handler f comb]
   (after handler (fn [response] (comb response (f response))))))

(defn effect-after
  "Like [[after]] but discard the result of `f`.

  ```clojure
  (effect-after handler log)
  ```"
  [handler f]
  (after handler f discard-result))

(defn after-async
  "Like [[after]] but with an asynchronous function `af`.
  `af` has two arities:
  (fn af [x]) returns a deref-able result on unary call.
  (fn af [x success fail]) take success and fail continuations.
  If the synchronous arity of the handler is never called"
  ([handler af]
   (fn
     ([request]
      @(af (handler request)))
     ([request respond raise]
      (handler request (fn [response] (af response respond raise)) raise))))
  ([handler af comb]
   (fn
     ([request]
      (let [response (handler request)]
        (comb response @(af response))))
     ([request respond raise]
      (handler request (fn [response]
                         (af response
                             (fn [result]
                               (respond (comb response result)))
                             raise)) raise)))))

(defn effect-after-async
  [handler af]
  (after-async handler af discard-result))

(defn around
  "Returns a handler surrounded by context returned by `enter` on request
  which returns `leave` applied to the result and context."
  ([handler enter leave]
   (fn
     ([request]
      (let [context (enter request)]
        (leave (handler request) context)))
     ([request respond raise]
      (let [context (enter request)]
        (handler request (before respond #(leave % context)) raise))))))

(defn around-long
  "Like [[around]] but the context returned by `enter` is a primitive long."
  ([handler ^IFn$OL enter ^IFn$OLO leave]
   (fn
     ([^Object request]
      (let [context (.invokePrim enter request)]
        (.invokePrim leave (handler request) context)))
     ([^Object request respond raise]
      (let [context (.invokePrim enter request)]
        (handler request (before respond #(.invokePrim leave % context)) raise))))))

(defn around-async
  "Returns a handler surrounded by context returned by `enter` on request
  which returns `leave` applied to the result and context."
  ([handler enter leave]
   (fn
     #_([request] ;; TODO
      (let [context (enter request)]
        (leave (handler request) context)))
     ([request respond raise]
      (enter
       request
       (fn [context]
         (handler
          request
          (fn [response] (leave response context respond raise))
          raise))
       raise)))))
