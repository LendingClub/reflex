package org.lendingclub.rx.guava;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.subjects.PublishSubject;

public class EventBusAdapter<T> {

	static Logger logger = LoggerFactory.getLogger(EventBusAdapter.class);
	EventBus bus;
	PublishSubject<T> publishSubject;

	Class<? extends Object> filterClass;

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
		bus.register(emitter);

		return emitter;
	}

	@Subscribe
	void receive(T obj) {

		if (publishSubject != null) {
			if (obj != null && filterClass.isInstance(obj)) {
				publishSubject.onNext(obj);
			}
			
		}

	}

	public Observable<T> getObservable() {
		return publishSubject;
	}
}
