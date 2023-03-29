(ns bsless.ring.middleware.tools-test
  (:require
   [clojure.test :as t]
   [bsless.ring.middleware.tools :as mwt])
  (:import
   (java.util.function Function)
   (java.util.concurrent CompletableFuture)))

(defn trace
  [request k]
  (update request :trace (fnil #(conj % k) [])))

(defn handler
  ([request]
   (trace request :handler))
  ([request respond _raise]
   (respond (trace request :handler))))

(defn spam [x] {:request x})

(defn spam-async
  ([x] (future (spam x)))
  ([x sk _fk]
   (.thenApplyAsync
    (CompletableFuture/completedStage
     (spam x))
    (reify Function
      (apply [_ ret] (sk ret))))))

(defn comb-before [request ret] (assoc request :before ret))

(t/deftest before
  (t/testing "blocking"
    (t/is
     (= {:trace [1 :handler]
         :before {:request {:trace [1]}}}
        ((mwt/before handler spam comb-before)
         {:trace [1]})))
    (t/testing "non blocking"
      (t/is
       (= {:trace [1 :handler]
           :before {:request {:trace [1]}}}
          ((mwt/before handler spam comb-before)
           {:trace [1]}
           identity
           identity))))))

(t/deftest effect-before
  (t/testing "blocking"
    (let [a (atom [])]
      (t/is
       (= {:trace [1 :handler]}
          ((mwt/effect-before handler (fn [req] (swap! a conj req)))
           {:trace [1]})))
      (t/is (= [{:trace [1]}] @a)))
    (t/testing "non blocking"
      (let [a (atom [])]
        (t/is
         (= {:trace [1 :handler]}
            ((mwt/effect-before handler (fn [req] (swap! a conj req)))
             {:trace [1]}
             identity
             identity)))
        (t/is (= [{:trace [1]}] @a))))))

(t/deftest before-async
  (t/testing "2-arity"
    (t/testing "blocking"
      (t/is
       (= {:trace [:handler]
           :request {:trace [1]}}
          ((mwt/before-async handler spam-async)
           {:trace [1]}))))
    (t/testing "non blocking"
      (t/is
       (= {:trace [:handler]
           :request {:trace [1]}}
          (let [p (promise)]
            ((mwt/before-async handler spam-async)
             {:trace [1]}
             (fn [x] (deliver p x))
             identity)
            @p)))))
  (t/testing "3-arity"
    (t/testing "blocking"
      (t/is
       (= {:trace [1 :handler]
           :before {:request {:trace [1]}}}
          ((mwt/before-async handler spam-async comb-before)
           {:trace [1]}))))
    (t/testing "non blocking"
      (t/is
       (= {:trace [1 :handler]
           :before {:request {:trace [1]}}}
          (let [p (promise)]
            ((mwt/before-async handler spam-async comb-before)
             {:trace [1]}
             (fn [x] (deliver p x))
             identity)
            @p))))))

(defn comb-after [request ret] (assoc request :after ret))

(t/deftest after
  (t/testing "blocking"
    (t/is
     (= {:trace [1 :handler]
         :after {:request {:trace [1 :handler]}}}
        ((mwt/after handler spam comb-after)
         {:trace [1]}))))
  (t/testing "non blocking"
    (t/is
     (= {:trace [1 :handler]
         :after {:request {:trace [1 :handler]}}}
        ((mwt/after handler spam comb-after)
         {:trace [1]}
         identity
         identity)))))

(t/deftest effect-after
  (t/testing "blocking"
    (let [a (atom [])]
      (t/is
       (= {:trace [1 :handler]}
          ((mwt/effect-after handler (fn [req] (swap! a conj req)))
           {:trace [1]})))
      (t/is (= [{:trace [1 :handler]}] @a)))
    (t/testing "non blocking"
      (let [a (atom [])]
        (t/is
         (= {:trace [1 :handler]}
            ((mwt/effect-after handler (fn [req] (swap! a conj req)))
             {:trace [1]}
             identity
             identity)))
        (t/is (= [{:trace [1 :handler]}] @a))))))

(t/deftest after-async
  (t/testing "blocking"
    (t/is
     (= {:trace [1 :handler]
         :after {:request {:trace [1 :handler]}}}
        ((mwt/after-async handler spam-async comb-after)
         {:trace [1]}))))
  (t/testing "non blocking"
    (t/is
     (= {:trace [1 :handler]
         :after {:request {:trace [1 :handler]}}}
        (let [p (promise)]
          ((mwt/after-async handler spam-async comb-after)
           {:trace [1]}
           (fn [x] (deliver p x))
           identity)
          @p)))))
