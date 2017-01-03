package org.lendingclub.reflex.operator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.SafeObserver;

public class ExceptionHandlers {

	static Logger logger = LoggerFactory.getLogger(ExceptionHandlers.class);

	public static <T> Consumer<T> safeConsumer(Consumer<T> consumer) {

		return safeConsumer(consumer, LoggerFactory.getLogger(consumer.getClass()));
	}

	public static interface UncaughtSubscriberExceptionHandler extends BiConsumer<Throwable, Object> {

	}

	public static class ExceptionSafeConsumer<T> implements Consumer<T> {

		Consumer<T> actual;
		UncaughtSubscriberExceptionHandler uncaughtHandler;

		public ExceptionSafeConsumer(Consumer<T> actual, UncaughtSubscriberExceptionHandler handler) {
			Preconditions.checkNotNull(actual);
			this.actual = actual;
			if (uncaughtHandler == null) {
				uncaughtHandler = new ExceptionLogger(LoggerFactory.getLogger(actual.getClass()));
			} else {
				uncaughtHandler = handler;
			}
			Preconditions.checkNotNull(uncaughtHandler);
		}

		@Override
		public void accept(T t) throws Exception {
			try {
				actual.accept(t);
			} catch (Throwable e) {
				Exceptions.throwIfFatal(e);
				try {
					uncaughtHandler.accept(e, t);
				} catch (Throwable x) {
					Exceptions.throwIfFatal(e);
					logger.warn("problem with exception handler", e);
					logger.warn("actual exception", e);
				}
			}

		}

	}

	public static class ExceptionLogger implements UncaughtSubscriberExceptionHandler {

		Logger log;

		ExceptionLogger(Logger log) {
			Preconditions.checkNotNull(log);
			this.log = log;
		}

		@Override
		public void accept(Throwable t1, Object t2) {
			log.warn("problem processing element", t2);
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> Consumer<T> safeConsumer(Consumer<T> consumer, UncaughtSubscriberExceptionHandler h) {
		return (Consumer<T>) new ExceptionSafeConsumer(consumer, h);
	}

	public static <T> Consumer<T> safeConsumer(Consumer<T> actual, Logger logger) {

		return safeConsumer(actual, new ExceptionLogger(logger));
	}

	public static <T> Observer<T> safeObserver(Observer<T> observer) {
		return safeObserver(observer, new ExceptionLogger(LoggerFactory.getLogger(observer.getClass())));
	}

	public static <T> Observer<T> safeObserver(Observer<T> observer, Logger logger) {
		return safeObserver(observer, new ExceptionLogger(logger));
	}

	public static <T> Observer<T> safeObserver(final Observer<T> observer,
			UncaughtSubscriberExceptionHandler handler) {

		Preconditions.checkNotNull(observer);
		if (handler == null) {
			handler = new ExceptionLogger(LoggerFactory.getLogger(observer.getClass()));
		}
		
		Preconditions.checkNotNull(handler);
		final UncaughtSubscriberExceptionHandler exceptionHandler = handler;
		Observer<T> wrapper = new Observer<T>() {
			Observer<T> x = observer;

			@Override
			public void onSubscribe(Disposable d) {
				x.onSubscribe(d);
			}

			@Override
			public void onNext(T t) {
				try {
					observer.onNext(t);
				} catch (Throwable e) {
					Exceptions.throwIfFatal(e);
					try {

						exceptionHandler.accept(e, t);

					} catch (Throwable x) {
						Exceptions.throwIfFatal(e);
						logger.warn("problem with exception handler", x);
						logger.warn("actual exception", e);
					}
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
		return new SafeObserver<T>(wrapper);
	}

}
