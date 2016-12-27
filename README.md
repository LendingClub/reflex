# rx-util

A set of utility classes for working with RxJava 2.x.

* A binding to Guava's EventBus
* A bounded work queue implemented as Observables
* A set of convenience Predicates



## Guava EventBus Obeservable

[EventBusAdapter](src/main/java/org/lendingclub/rx/guava/EventBusAdapter.java) provides a simple Observable binding to Guava's [EventBus](https://github.com/google/guava/wiki/EventBusExplained).

The following is a simple hello-world example.  [EventBusAdapter](src/main/java/org/lendingclub/rx/guava/EventBusAdapter.java) subscribes to the EventBus and exposes it as an Observable.

```java
EventBus eventBus = new EventBus();
    
Observable<Object> observable = EventBusAdapter.toObservable(eventBus);

observable.subscribe(it -> {
    System.out.println("Hello, "+it);
});
eventBus.post("world");
```

As a convenience, it is possible to filter events into a type-safe Observable.

```java
Observable<MyEvent> observable = EventBusAdapter.toObservable(eventBus, MyEvent.class);
```

## Bounded Work Queue

Sometimes it can be hard to reason with the Reactive threading model.  [WorkQueue](src/main/java/org/lendingclub/rx/queue/WorkQueue.java) provides a simple way to put a BlockingQueue/ThreadPoolExecutor 
between a source Observable and an Observable that acts as the worker.

In the following, the Observable consisting of the range of values [0..99] is subscribed-to by a work queue the processes the values in sequence:

```java
WorkQueue<Integer> queue = new WorkQueue<Integer>();

queue.getObservable().subscribe(it -> {
    System.out.println("processing "+it+" in "+Thread.currentThread());
});
    
Observable.range(0, 100).subscribe(queue);
```

WorkQueueObserver exposes a number of options availbale on the underlying Executor:

```java
WorkQueue<Integer> queue = new WorkQueue<Integer>()
    .withCoreThreadPoolSize(5)
    .withQueueSize(5000)
    .withThreadName("my-thread-%d")
    .withThreadTimeout(30, TimeUnit.SECONDS);
```

## Convenience Predicates

The [Predicates](src/main/java/org/lendingclub/rx/predicate/Predicates.java) class has a convenience method that applies Jackson's fluent path evaluation as a predicate.

The following will filter out only Jacckson JsonNode objects that have a ```foo``` attribute with a value of ```bar```:

```java
JsonNode data = ...;

Observable.just(data).filter(Predicates.json(json -> {
    return json.path("foo").asText().equals("bar");
}));
```

It can sometimes be more convenient to filter with ```flatMap```, which does type conversion.

```java
Observable.just(n0).flatMap(FlatMapFilters.json(json -> {
    return json.path("foo").asText().equals("bar");
}));
```

