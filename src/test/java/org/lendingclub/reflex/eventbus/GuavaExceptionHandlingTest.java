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

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuavaExceptionHandlingTest {

  Logger logger = LoggerFactory.getLogger(GuavaExceptionHandlingTest.class);
  ThreadPoolExecutor tpe;

  @Test
  public void testIt() throws InterruptedException {

    int count = 50;
    UncaughtExceptionHandler h =
        new UncaughtExceptionHandler() {

          @Override
          public void uncaughtException(Thread t, Throwable e) {
            System.out.println(t + " " + e);
          }
        };

    LinkedBlockingDeque<Runnable> q = new LinkedBlockingDeque<>(500);
    ThreadFactory tf = new ThreadFactoryBuilder().build();
    tpe = new ThreadPoolExecutor(2, 2, 10, TimeUnit.SECONDS, q, tf);

    SubscriberExceptionHandler sh =
        new SubscriberExceptionHandler() {

          @Override
          public void handleException(Throwable arg0, SubscriberExceptionContext arg1) {
            //	logger.warn("received exception in context "+arg1,arg0);
            logger.info("received exception in context {} {}", arg1, arg0.toString());
          }
        };

    // SubscriberExceptonHandler does NOT change the behavior other than override the default
    // logging
    EventBus bus = new AsyncEventBus(tpe, sh);

    CountDownLatch latch = new CountDownLatch(count);
    Object test0 =
        new Object() {

          @Subscribe
          @AllowConcurrentEvents
          public void receive(String message) {
            throw new RuntimeException();
          }
        };
    Object test1 =
        new Object() {

          @Subscribe
          @AllowConcurrentEvents
          public void receive(String message) {
            System.out.println("receive " + message);
            latch.countDown();
          }
        };
    Object test2 =
        new Object() {

          @Subscribe
          @AllowConcurrentEvents
          public void receive(String message) {
            throw new RuntimeException();
          }
        };
    bus.register(test0);
    bus.register(test1);
    bus.register(test2);

    for (int i = 0; i < count; i++) {
      bus.post(Integer.toString(i));
    }

    Assertions.assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
  }

  @After
  public void cleanup() {
    tpe.shutdown();
  }
}
