package org.lendingclub.reflex.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.SafeObserver;

public class ExceptionHandlers {

	static Logger logger = LoggerFactory.getLogger(ExceptionHandlers.class);
	public static <T> Consumer<T> safeConsumer(Consumer<T> consumer) {

		return safeConsumer(consumer, LoggerFactory.getLogger(consumer.getClass()));
	}

	public  static interface UncaughtExceptionHandler {
		public void handleException(Throwable t, Object x);
	}
	public static class ExceptionLogger<T>  implements UncaughtExceptionHandler {

		Logger log;
		ExceptionLogger(Logger log) {
			this.log = log;
		}
		@Override
		public void handleException(Throwable t1, Object t2)  {
			log.warn("problem processing element",t1);
		
		}
		
	}
	public static <T> Consumer<T> safeConsumer(Consumer<T> consumer, UncaughtExceptionHandler h) {
		Consumer<T> wrapper = new Consumer<T>() {

		
			@Override
			public void accept(T t) throws Exception {
				try {
					consumer.accept(t);
				} catch (Throwable e) {
					Exceptions.throwIfFatal(e);
					try {
						h.handleException(e,t);
					} catch (Throwable x) {
						Exceptions.throwIfFatal(e);
						logger.warn("problem with exception handler",e);
					}
				}

			}

		};
		return wrapper;
	}

	public static <T> Consumer<T> safeConsumer(Consumer<T> actual, Logger logger) {

		return safeConsumer(actual,new ExceptionLogger<T>(logger));
	}
	public static <T> Observer<T> safeObserver(Observer<T> observer) {
		return safeObserver(observer, new ExceptionLogger<>(LoggerFactory.getLogger(observer.getClass())));
	}
	public static <T> Observer<T> safeObserver(Observer<T> observer, Logger logger) {
		return safeObserver(observer, new ExceptionLogger<>(logger));
	}


	public static <T> Observer<T> safeObserver(final Observer<T> observer,  UncaughtExceptionHandler handlerFunction) {

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
						handlerFunction.handleException(e,t);
					} catch (Throwable x) {
						Exceptions.throwIfFatal(e);
						
						logger.warn("problem with exception handler",x);
						logger.warn("actual exception",e);
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
		return new SafeObserver(wrapper);
	}

}
