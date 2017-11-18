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
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.reactivex.Observable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventBusAdapter<T> {

  static Logger logger = LoggerFactory.getLogger(EventBusAdapter.class);
  EventBus bus;

  Subject<T> subject;
  Observable<T> observableWithCounter;

  AtomicLong counter = new AtomicLong();

  Class<? extends Object> filterClass;

  class CounterFunction implements Function<T, T> {

    @Override
    public T apply(T t) throws Exception {
      counter.incrementAndGet();
      return (T) t;
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> Observable<T> toObservable(EventBus bus, Class<? extends T> clazz) {
    return (Observable<T>) createAdapter(bus, clazz).getObservable();
  }

  public static Observable<Object> toObservable(EventBus bus) {
    return (Observable<Object>) createAdapter(bus, Object.class).getObservable();
  }

  public static EventBusAdapter<? extends Object> createAdapter(EventBus bus) {
    return createAdapter(bus, Object.class);
  }

  @SuppressWarnings("unchecked")
  public static <T> EventBusAdapter<T> createAdapter(EventBus bus, Class<? extends T> clazz) {

    logger.info("creating adapter for eventBus={} type={}", bus, clazz);
    EventBusAdapter<T> emitter = new EventBusAdapter<T>();
    emitter.filterClass = clazz;
    emitter.bus = bus;
    emitter.subject = (Subject<T>) PublishSubject.create().toSerialized();
    emitter.observableWithCounter = emitter.subject.map(emitter.new CounterFunction());
    bus.register(emitter);

    return emitter;
  }

  @Subscribe
  @AllowConcurrentEvents
  void receive(T obj) {

    if (subject != null) {
      if (obj != null && filterClass.isInstance(obj)) {
        try {
          subject.onNext(obj);
        } catch (Throwable e) {
          Exceptions.throwIfFatal(e);
          logger.warn("problem", e);
        }
      }
    }
  }

  public AtomicLong getMessageCount() {
    return counter;
  }

  public Observable<T> getObservable() {
    return observableWithCounter;
  }
}
