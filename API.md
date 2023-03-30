# bsless.ring.middleware.tools 





## `after`
``` clojure

(after handler f)
(after handler f comb)
```


Return a new handler which calls `f` on the response from `handler`.
  Optionally takes `comb`, (fn [request result]) -> result, to allow
  separation of `f`'s calculation from how its result should be passed
  to the handler in the new response.

  For example:
  ```clojure
  (after handler (fn [response] (doto response log)))
  ```
<br><sub>[source](null/blob/null/src/bsless/ring/middleware/tools.clj#L64-L81)</sub>
## `after-async`
``` clojure

(after-async handler af)
(after-async handler af comb)
```


Like [[after]] but with an asynchronous function `af`.
  `af` has two arities:
  (fn af [x]) returns a deref-able result on unary call.
  (fn af [x success fail]) take success and fail continuations.
  If the synchronous arity of the handler is never called
<br><sub>[source](null/blob/null/src/bsless/ring/middleware/tools.clj#L92-L114)</sub>
## `around`
``` clojure

(around handler enter leave)
```


Returns a handler surrounded by context returned by `enter` on request
  which returns `leave` applied to the result and context.
<br><sub>[source](null/blob/null/src/bsless/ring/middleware/tools.clj#L120-L130)</sub>
## `around-async`
``` clojure

(around-async handler enter leave)
```


Like [[around]] but `enter` and `leave` are both asynchronous functions
  that take success and fail callbacks.
  Currently only implemented for non-blocking handlers.
<br><sub>[source](null/blob/null/src/bsless/ring/middleware/tools.clj#L143-L158)</sub>
## `around-long`
``` clojure

(around-long handler enter leave)
```


Like [[around]] but the context returned by `enter` is a primitive long.
<br><sub>[source](null/blob/null/src/bsless/ring/middleware/tools.clj#L132-L141)</sub>
## `before`
``` clojure

(before handler f)
(before handler f comb)
```


Return a new handler which calls `f` on the request before passing it to `handler`.
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
  ```
<br><sub>[source](null/blob/null/src/bsless/ring/middleware/tools.clj#L7-L28)</sub>
## `before-async`
``` clojure

(before-async handler af)
(before-async handler af comb)
```


Invoke async function `af` before the handler, similarly to [[before]].
  `af` can have two arities which will be invoked depending on how the handler is called:
  - (fn af [x]) returns a deref-able result
  - (fn af [x on-success on-failure]) takes continuation callbacks.
  The handler is invoked in the continuation.
  Optionally takes (fn comb [request result]) -> request as an optional argument.
<br><sub>[source](null/blob/null/src/bsless/ring/middleware/tools.clj#L38-L56)</sub>
## `effect-after`
``` clojure

(effect-after handler f)
```


Like [[after]] but discard the result of `f`.

  ```clojure
  (effect-after handler log)
  ```
<br><sub>[source](null/blob/null/src/bsless/ring/middleware/tools.clj#L83-L90)</sub>
## `effect-after-async`
``` clojure

(effect-after-async handler af)
```

<sub>[source](null/blob/null/src/bsless/ring/middleware/tools.clj#L116-L118)</sub>
## `effect-before`
``` clojure

(effect-before handler f)
```


Invoke an effectful function `f` before continuing with `handler`.
  Its result is discarded.
<br><sub>[source](null/blob/null/src/bsless/ring/middleware/tools.clj#L32-L36)</sub>
## `effect-before-async`
``` clojure

(effect-before-async handler af)
```


Invoke an asynchronous effect before the handler, similarly to [[effect-before]].
  The handler will be called only AFTER the effect completes.
<br><sub>[source](null/blob/null/src/bsless/ring/middleware/tools.clj#L58-L62)</sub>
