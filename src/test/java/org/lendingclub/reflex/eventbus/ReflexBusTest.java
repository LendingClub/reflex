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

import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Preconditions;
import org.junit.Test;
import org.lendingclub.reflex.concurrent.ReflexExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReflexBusTest {

  Logger logger = LoggerFactory.getLogger(ReflexBusTest.class);
  CountDownLatch latch;

  @Test
  public void testObservableWithDirectExecutor() throws InterruptedException {
    testObservableWithExecutor(MoreExecutors.directExecutor(), 5);
  }

  @Test
  public void testObservableWithReflexExecutorSingleThread() throws InterruptedException {
    testObservableWithExecutor(ReflexExecutors.newThreadPoolExecutorBuilder().build(), 5);
  }

  @Test
  public void testObservableWithReflexExecutorWithMultipleThreads() throws InterruptedException {
    testObservableWithExecutor(
        ReflexExecutors.newThreadPoolExecutorBuilder().withThreadPoolSize(2).build(), 5);
  }

  @Test
  public void testConcurrentSubscriberWithDirectExecutor() throws InterruptedException {
    testConcurrentSubscriberWithExecutor(MoreExecutors.directExecutor(), 5);
  }

  @Test
  public void testConcurrentSubscriberWithReflexExecutorSingleThread() throws InterruptedException {
    testConcurrentSubscriberWithExecutor(ReflexExecutors.newThreadPoolExecutorBuilder().build(), 5);
  }

  @Test
  public void testConcurrentSubscriberWithReflexExecutorWithMultipleThreads()
      throws InterruptedException {
    testConcurrentSubscriberWithExecutor(
        ReflexExecutors.newThreadPoolExecutorBuilder().withThreadPoolSize(3).build(), 20);
  }

  public void testConcurrentSubscriberWithExecutor(Executor executor, int count)
      throws InterruptedException {
    logger.info("\n\n");
    logger.info("****** Testing ReflexBus with {} *****", executor);

    // do not be confused by the fact that we are using DirectExecutor here.  This is the executor
    // for ReflexBus, not the subscriber.

    ReflexBus bus = ReflexBus.newBuilder().withExecutor(MoreExecutors.directExecutor()).build();

    bus.createConcurrentSubscriber()
        .withExecutor(executor)
        .subscribe(
            event -> {
              logger.info("processed {} on {}", event, Thread.currentThread());
              Thread.sleep(300);
              latch.countDown();
            });
    setLatch(count);

    for (int i = 0; i < count; i++) {
      String message = "Event " + i;
      bus.post(message);
      logger.info("posted {} on {}", message, Thread.currentThread());
    }

    waitForLatch();
  }

  public void testObservableWithExecutor(Executor executor, int count) throws InterruptedException {

    // Keep in mind that EXECUTIONS WILL BE SERIALIZED.
    // The Observable Contract says that onNext() invocation *must be serialized*.  This can feel
    // wrong, but it is absolutely correct.  If you requre paralleism.
    logger.info("\n\n");
    logger.info("****** Testing ReflexBus with {} *****", executor);

    ReflexBus bus = ReflexBus.newBuilder().withExecutor(executor).build();

    setLatch(count);

    bus.createObservable()
        .subscribe(
            x -> {
              logger.info("processed {} on {}", x, Thread.currentThread());
              Thread.sleep(300);
              latch.countDown();
            });

    for (int i = 0; i < count; i++) {
      String message = "Event " + i;
      bus.post(message);
      logger.info("posted {} on {}", message, Thread.currentThread());
    }

    waitForLatch();
  }

  void setLatch(int count) {
    latch = new CountDownLatch(count);
  }

  void waitForLatch() throws InterruptedException {
    Preconditions.checkNotNull(latch);
    Assertions.assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
  }
}
