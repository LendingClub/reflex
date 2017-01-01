# reflex

[![CircleCI](https://circleci.com/gh/LendingClub/reflex.svg?style=svg)](https://circleci.com/gh/LendingClub/reflex)
[ ![Download](https://api.bintray.com/packages/lendingclub/OSS/reflex/images/download.svg) ](https://bintray.com/lendingclub/OSS/reflex/_latestVersion)
[![CodeCov](https://codecov.io/github/LendingClub/reflex/coverage.svg)](https://codecov.io/github/LendingClub/reflex)

Reflex is a set of utilities for working with [RxJava 2.x](https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0) and [Reactive Streams](http://www.reactive-streams.org/)


* A binding to Guava's EventBus
* ConcurrentConsumers class for parallelizing consumer execution
* A set of convenience Predicates
* AWS SQS Support

## Guava EventBus Observable

[EventBusAdapter](src/main/java/org/lendingclub/reflex/guava/EventBusAdapter.java) provides a simple Observable binding to Guava's [EventBus](https://github.com/google/guava/wiki/EventBusExplained).

The following is a simple hello-world example.  [EventBusAdapter](src/main/java/org/lendingclub/reflex/guava/EventBusAdapter.java) subscribes to the EventBus and exposes it as an Observable.

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

## Concurrent Consumers

It may be surprising, but reactive streams are inherenty single-threaded.  The Observable Contract states that only one thread
may call ```onNext()```. 

In many cases, you may have a heavy-weight Consumer that is compute or I/O-intensive.  Achieving Consumer parallelism with RxJava operators is
not difficult, but it is very unintuitive.  What you need to understand is that it is *simply impossible* with a single Observable.  What you need
to do is to create a new Observable sequence from within a ```flatMap``` operator.  This code ends up being a bit diffiuclt to understand.

ConcurrentConsumers to the rescue!

The following code prinnts out the values 0..4 in parallel, each within its own thread.

```java
ConcurrentConsumers.subscribeParallel(
    Observable.range(0, 5),
    Schedulers.newThread(),
    val -> {
        System.out.println("processing "+val+" in "+Thread.currentThread());
    }
);
```

Output:

```bash
processing 0 in Thread[RxNewThreadScheduler-1,5,main]
processing 4 in Thread[RxNewThreadScheduler-5,5,main]
processing 2 in Thread[RxNewThreadScheduler-3,5,main]
processing 1 in Thread[RxNewThreadScheduler-2,5,main]
processing 3 in Thread[RxNewThreadScheduler-4,5,main]
```	

## Convenience Predicates

The [Predicates](src/main/java/org/lendingclub/reflex/predicate/Predicates.java) class has a convenience method that applies Jackson's fluent path evaluation as a predicate.

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


[SQSAdapter](src/main/java/org/lendingclub/reflex/aws/sqs/SQSAdapter.java) simplifies the task of reading from an SQS queue and processing it with Rx goodness.

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
