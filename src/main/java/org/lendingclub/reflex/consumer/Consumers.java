package org.lendingclub.reflex.consumer;

import org.lendingclub.reflex.exception.ExceptionHandlers;
import org.slf4j.Logger;

import io.reactivex.Observer;
import io.reactivex.functions.Consumer;

public class Consumers {

	
	@Deprecated
	public static <T> Consumer<T> safeConsumer(Consumer<T> consumer) {
	
		return ExceptionHandlers.safeConsumer(consumer);
	}

	@Deprecated
	public static <T> Consumer<T> safeConsumer(Consumer<T> consumer, Logger logger) {

		return ExceptionHandlers.safeConsumer(consumer, logger);
	}

	@Deprecated
	public static <T> Observer<T> safeObserver(Observer<T> observer) {
		return ExceptionHandlers.safeObserver(observer);
	}
	@Deprecated
	public static <T> Observer<T> safeObserver(final Observer<T> observer, Logger logger) {
		return ExceptionHandlers.safeObserver(observer,logger);
	}

}
