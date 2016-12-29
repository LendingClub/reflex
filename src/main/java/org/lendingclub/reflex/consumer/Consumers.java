package org.lendingclub.reflex.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Observer;
import io.reactivex.functions.Consumer;

public class Consumers {

	public static <T> Consumer<T> safeConsumer(Consumer<T> consumer) {

		return safeConsumer(consumer, LoggerFactory.getLogger(consumer.getClass()));
	}

	public static <T> Consumer<T> safeConsumer(Consumer<T> consumer, Logger logger) {

		Consumer<T> wrapper = new Consumer<T>() {

			@Override
			public void accept(T t) throws Exception {
				try {
					consumer.accept(t);
				} catch (Exception e) {
					logger.error("exception in consumer", e);
				}

			}

		};
		return wrapper;
	}
	
	public static <T> Consumer<T> safeConsumer(Observer<T> observer) {

		return safeConsumer(observer, LoggerFactory.getLogger(observer.getClass()));
	}
	public static <T> Consumer<T> safeConsumer(Observer<T> observer, Logger logger) {

		Consumer<T> wrapper = new Consumer<T>() {

			@Override
			public void accept(T t) throws Exception {
				try {
					observer.onNext(t);
				} catch (Exception e) {
					logger.error("exception in consumer", e);
				}

			}

		};
		return wrapper;
	}

}
