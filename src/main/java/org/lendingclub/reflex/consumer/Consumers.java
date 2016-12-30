package org.lendingclub.reflex.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
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

	public static <T> Observer<T> safeObserver(Observer<T> observer) {
		return safeObserver(observer, LoggerFactory.getLogger(observer.getClass()));
	}

	public static <T> Observer<T> safeObserver(final Observer<T> observer, Logger logger) {

		Observer<T> wrapper = new Observer<T>() {
			Observer<T> x = observer;

			@Override
			public void onSubscribe(Disposable d) {
				x.onSubscribe(d);

			}

			@Override
			public void onNext(T t) {
				try {
					x.onNext(t);
				} catch (Exception e) {
					logger.error("exception in Observer", e);
				}

			}

			@Override
			public void onError(Throwable e) {
				x.onError(e);

			}

			@Override
			public void onComplete() {
				x.onComplete();

			}

		};
		return wrapper;
	}

}
