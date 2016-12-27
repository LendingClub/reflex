# rx-util

A set of utility classes for working with RxJava 2.x.

* A binding to Guava's EventBus
* A bounded work queue implemented as Observables
* A set of convenience Predicates
* AWS SQS Support

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
between a source Observable and an Observable that acts as the worker.  It may go against the Reactive Manifesto, but it is simple and clear.  With the reactive Schedulers, ```subscribeOn```, and ```observeOn``` it is not always so clear what is happening, so mistakes are easy to make.

In the following example, the Observable consisting of the range of values [0..99] is subscribed-to by a work queue the processes the values in sequence in a separate thread:

```java
WorkQueue<Integer> queue = new WorkQueue<Integer>();

queue.getObservable().subscribe(it -> {
    System.out.println("processing "+it+" in "+Thread.currentThread());
});
    
Observable.range(0, 100).subscribe(queue);
```

WorkQueueObserver exposes a number of options availbale on the underlying Executor, such as the queue size, the RejectedExecutionHandler policy, the concurrency in the thread pool, etc.:

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

## AWS Simple Queue Service (SQS) Support


[SQSAdapter](src/main/java/org/lendingclub/rx/aws/sqs/SQSAdapter.java) simplifies the task of reading from an SQS queue and processing it with Rx goodness.

```java
AmazonSQSClient client = new AmazonSQSClient(new DefaultAWSCredentialsProviderChain());

SQSAdapter adapter = new SQSAdapter()
    .withSQSClient(client)
    .withQueueUrl("https://sqs.us-west-2.amazonaws.com/123456789012/myqueue");

adapter.getObservable().flatMap(new SQSAdapter.SQSJsonMessageExtractor()).subscribe(c -> {
    System.out.println(c);
});

adapter.start();
```


In particular note ```SQSAdapter.SQSJsonMessageExtractor``` which extracts JSON message payloads.  If the message contains an SNS
envelope, it will unwrap that envelope as well.
