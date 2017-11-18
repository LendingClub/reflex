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

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import org.lendingclub.reflex.concurrent.ReflexExecutors.ThreadPoolExecutorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConcurrentSubscribers {

  static Logger logger = LoggerFactory.getLogger(ConcurrentSubscribers.class);
  static AtomicLong instanceCount = new AtomicLong();

  public static <T> void subscribeParallel(Observable<T> x, Scheduler scheduler, Consumer<T> c) {
    x.flatMap(
            val -> {
              Observable.just(val).subscribeOn(scheduler).subscribe(c);
              return Observable.just(val);
            })
        .subscribe(dummy -> {});
  }

  public static <T> void subscribeParallel(Observable<T> x, Scheduler scheduler, Observer<T> c) {
    x.flatMap(
            val -> {
              Observable.just(val).subscribeOn(scheduler).subscribe(c);
              return Observable.just(val);
            })
        .subscribe(dummy -> {});
  }

  public static <T> void subscribeParallel(Observable<T> x, Executor executor, Observer<T> c) {

    subscribeParallel(x, Schedulers.from(executor), c);
  }

  public static <T> void subscribeParallel(Observable<T> x, Executor executor, Consumer<T> c) {

    subscribeParallel(x, Schedulers.from(executor), c);
  }

  public static class DefaultUncaughtExceptionHandler implements UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
      Logger logger = LoggerFactory.getLogger(DefaultUncaughtExceptionHandler.class);
      logger.warn("uncaught exception", e);
    }
  }

  public static class ConcurrentSubscriber<T> {

    Observable<T> source;

    Executor executor;
    Scheduler scheduler;

    Consumer<ThreadPoolExecutorBuilder> executorConfigurer;

    ConcurrentSubscriber(Observable<T> source) {
      this.source = source;
    }

    public ConcurrentSubscriber<T> withNewExecutor(Consumer<ThreadPoolExecutorBuilder> c) {

      executorConfigurer = c;

      return this;
    }

    public ConcurrentSubscriber<T> withScheduler(Scheduler scheduler) {
      this.scheduler = scheduler;
      return this;
    }

    public ConcurrentSubscriber<T> withExecutor(Executor executor) {
      this.executor = executor;
      return this;
    }

    public Optional<Executor> getExecutor() {
      return Optional.ofNullable(executor);
    }

    public ConcurrentSubscriber<T> subscribe(Consumer<T> consumer) {

      if (scheduler != null && executor != null) {
        throw new IllegalArgumentException(
            "withScheduler() and withExecutor() are mutually exclusive");
      }

      if (scheduler == null && executor == null) {

        if (executorConfigurer != null) {
          try {
            ThreadPoolExecutorBuilder b = new ThreadPoolExecutorBuilder();
            b.withRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy())
                .withUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler());

            executorConfigurer.accept(b);
            executor = b.build();
            logger.info("created new executor for subscription: " + executor);
          } catch (RuntimeException e) {
            throw e;
          } catch (Exception e) {
            throw new IllegalArgumentException(e);
          }
        } else {
          throw new IllegalArgumentException(
              "must provide one of: executor, scheduler or set withNewExecutor()");
        }
      }

      if (scheduler == null) {

        scheduler = Schedulers.from(executor);
      }

      subscribeParallel(source, scheduler, consumer);
      return this;
    }
  }

  public static <T> ConcurrentSubscriber<T> createConcurrentSubscriber(Observable<T> source) {
    return new ConcurrentSubscriber<>(source);
  }
}
