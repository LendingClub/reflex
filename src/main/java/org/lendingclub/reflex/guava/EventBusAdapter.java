package org.lendingclub.reflex.guava;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.subjects.PublishSubject;

public class EventBusAdapter<T> {

	static Logger logger = LoggerFactory.getLogger(EventBusAdapter.class);
	EventBus bus;
	PublishSubject<T> publishSubject;
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
	
	@SuppressWarnings("unchecked")
	public static  Observable<Object> toObservable(EventBus bus) {
		return (Observable<Object>) createAdapter(bus, Object.class).getObservable();
	}
	
	public static EventBusAdapter<? extends Object> createAdapter(EventBus bus) {
		return createAdapter(bus,Object.class);
	}
	
	public static <T> EventBusAdapter<? extends T> createAdapter(EventBus bus, Class<? extends T> clazz) {

		logger.info("creating adapter for eventBus={} type={}",bus,clazz);
		EventBusAdapter<T> emitter = new EventBusAdapter<T>();
		emitter.filterClass = clazz;
		emitter.bus = bus;
		emitter.publishSubject = PublishSubject.create();
		emitter.observableWithCounter = emitter.publishSubject.map(emitter.new CounterFunction());
		bus.register(emitter);

		return emitter;
	}

	@Subscribe
	@AllowConcurrentEvents
	void receive(T obj) {

		if (publishSubject != null) {
			if (obj != null && filterClass.isInstance(obj)) {
				try {
					publishSubject.onNext(obj);
				}
				catch (RuntimeException e) {
					logger.warn("problem",e);
				}
			}
			
		}

	}

	
	public Observable<T> getObservable() {
		return observableWithCounter;
	}
}
