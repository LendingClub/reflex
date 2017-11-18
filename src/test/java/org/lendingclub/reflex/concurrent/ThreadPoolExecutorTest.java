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
package org.lendingclub.reflex.concurrent;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ThreadPoolExecutorTest {

  CountDownLatch latch;
  CountDownLatch exceptionHandlerLatch;

  class TestRunnable implements Runnable {

    int c;

    TestRunnable(int i) {
      c = i;
    }

    @Override
    public void run() {
      if (c == 5 || c == 6) {
        System.out.println("fail");
        throw new RuntimeException();
      }
      latch.countDown();
      System.out.println("run " + c + " " + Thread.currentThread());
    }
  }

  @Test
  public void testIt() throws InterruptedException {

    UncaughtExceptionHandler h =
        new UncaughtExceptionHandler() {

          @Override
          public void uncaughtException(Thread t, Throwable e) {
            System.out.println(t + " " + e);
            exceptionHandlerLatch.countDown();
          }
        };
    exceptionHandlerLatch = new CountDownLatch(2);
    LinkedBlockingDeque<Runnable> q = new LinkedBlockingDeque<>(100);
    ThreadFactory tf = new ThreadFactoryBuilder().setUncaughtExceptionHandler(h).build();
    ThreadPoolExecutor tpe = new ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS, q, tf);

    latch = new CountDownLatch(8);
    for (int i = 0; i < 10; i++) {

      tpe.execute(new TestRunnable(i));
    }

    Assertions.assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    Assertions.assertThat(exceptionHandlerLatch.await(10, TimeUnit.SECONDS)).isTrue();
    tpe.shutdown();
  }
}
