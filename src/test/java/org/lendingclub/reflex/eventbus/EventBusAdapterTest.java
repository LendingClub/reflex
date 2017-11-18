/**
 * Copyright 2017 Lending Club, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lendingclub.reflex.eventbus;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import io.reactivex.Observable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Test;
import org.lendingclub.reflex.concurrent.ConcurrentSubscribers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventBusAdapterTest {

  Logger logger = LoggerFactory.getLogger(EventBusAdapterTest.class);

  ExecutorService executor;

  @After
  public void cleanup() {
    if (executor != null) {
      executor.shutdown();
      executor = null;
    }
  }

  @Test
  public void test() throws InterruptedException {
    int count = 10;
    executor = Executors.newFixedThreadPool(1);

    EventBus bus = new AsyncEventBus(executor);

    List<String> stringList = Lists.newArrayList();

    EventBusAdapter<String> emitter = EventBusAdapter.createAdapter(bus, String.class);

    AtomicReference<Thread> workerThread = new AtomicReference<Thread>(null);
    CountDownLatch latch = new CountDownLatch(count * 2);
    ConcurrentSubscribers.subscribeParallel(
        emitter.getObservable(),
        executor,
        c -> {
          logger.info("foo " + c);
          Thread.sleep(300);
          latch.countDown();
          if (workerThread.get() != null) {
            Assertions.assertThat(Thread.currentThread()).isSameAs(workerThread.get());
          } else {
            workerThread.set(Thread.currentThread());
          }
        });

    long t0 = System.currentTimeMillis();
    for (int i = 0; i < count; i++) {
      bus.post("A " + i);
      bus.post("B " + i);
    }
    long t1 = System.currentTimeMillis();

    Assertions.assertThat(t1 - t0).isLessThan(100);

    Assertions.assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();

    long t2 = System.currentTimeMillis();

    // This verifies that we are using a single-threaded work queue.
    Assertions.assertThat(t2 - t0).isGreaterThan(5000);
  }

  @Test
  public void testSimple() {
    EventBus eventBus = new EventBus();

    Observable<Object> observable = EventBusAdapter.toObservable(eventBus, String.class);

    observable.subscribe(
        it -> {
          logger.info("Hello, {}", it);
        });

    eventBus.post("world");
    eventBus.post(123);
  }

  /**
   * This tests that @AllowCurrentEvents is enabled on the @Subscribe method of the EventBusAdapter.
   * If it is NOT enabled, the Guava EventBus will serialize dispatching. This test verifies that
   * this is not the case by timing the execution. If serialized, the total time process all
   * observable subscriptions will far exceed a known limit.
   *
   * @throws InterruptedException
   */
  @Test
  public void testConcurrentEvents() throws InterruptedException {
    ExecutorService x = Executors.newFixedThreadPool(10);
    try {
      EventBus eventBus = new AsyncEventBus(x);
      Observable<Object> observable1 = EventBusAdapter.toObservable(eventBus, String.class);
      Observable<Object> observable2 = EventBusAdapter.toObservable(eventBus, String.class);

      int count = 100;
      CountDownLatch latch = new CountDownLatch(count);

      ConcurrentSubscribers.subscribeParallel(
          observable1,
          x,
          it -> {
            logger.info("subscriber " + it);
            latch.countDown();
            Thread.sleep(50);
          });

      ConcurrentSubscribers.subscribeParallel(
          observable2,
          x,
          it -> {
            logger.info("subscriber2 " + it);
            latch.countDown();
            Thread.sleep(30);
          });

      long t0 = System.currentTimeMillis();
      logger.info("start");
      for (int i = 0; i < count; i++) {
        eventBus.post("test " + i);
      }
      Assertions.assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue();
      logger.info("stop");
      long t1 = System.currentTimeMillis();

      Assertions.assertThat(t1 - t0).isLessThan(10000);
    } finally {
      x.shutdown();
    }
  }
}
