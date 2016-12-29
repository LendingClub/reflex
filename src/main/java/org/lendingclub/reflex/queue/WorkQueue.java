package org.lendingclub.reflex.queue;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.UncaughtExceptionHandlers;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

/**
 * WorkQueueObserver connects an Rx Observable to a bounded work queue and
 * exposes the resulting work queue as an Observable.
 * 
 * @author rschoening
 *
 * @param <T>
 */
public class WorkQueue<T> implements Observer<T> {

	Logger logger = LoggerFactory.getLogger(WorkQueue.class);

	static AtomicInteger count = new AtomicInteger(0);

	String name = "QueueSubscriber-" + (count.getAndIncrement()) + "-%d";
	int queueSize = 1000;
	int coreThreadPoolSize = 1;
	int maxThreadPoolSize = 1;
	int time = 30;
	TimeUnit timeUnit = TimeUnit.SECONDS;
	RejectedExecutionHandler rejectedExecutionHandler = new MyRejectedExecutionHandler();
	LinkedBlockingDeque<Runnable> queue;
	ThreadPoolExecutor executor;
	UncaughtExceptionHandler uncaughtExceptionHandler = null;
	final PublishSubject<T> publishSubject = PublishSubject.create();

	public WorkQueue() {

	}

	class MyRejectedExecutionHandler extends ThreadPoolExecutor.DiscardPolicy {

		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {

			super.rejectedExecution(r, e);
			logger.error("rejected {} in {}", r, e);
		}

	};

	public ThreadPoolExecutor getThreadPoolExecutor() {
		Preconditions.checkState(executor != null, "WorkQueueObserver has not been started");
		return executor;
	}

	public WorkQueue<T> withQueueSize(int size) {
		assertNotStarted();
		this.queueSize = size;
		return this;
	}

	public WorkQueue<T> withCoreThreadPoolSize(int size) {
		assertNotStarted();
		this.coreThreadPoolSize = size;
		this.maxThreadPoolSize = Math.max(maxThreadPoolSize, this.coreThreadPoolSize);
		return this;
	}

	public WorkQueue<T> withRejectedExecutionHandler(RejectedExecutionHandler handler) {
		assertNotStarted();
		this.rejectedExecutionHandler = handler;
		return this;
	}

	public WorkQueue<T> withMaxThreadPoolSize(int size) {
		assertNotStarted();
		this.coreThreadPoolSize = size;
		return this;
	}

	public WorkQueue<T> withThreadTimeout(int time, TimeUnit timeUnit) {
		assertNotStarted();
		this.time = time;
		this.timeUnit = timeUnit;
		return this;
	}

	public WorkQueue<T> withThreadName(String name) {
		assertNotStarted();
		this.name = name;
		return this;
	}

	public Observable<T> getObservable() {
		return publishSubject;
	}

	protected synchronized void start() {
		if (executor == null) {
			queue = new LinkedBlockingDeque<>(queueSize);

			Preconditions.checkArgument(maxThreadPoolSize >= coreThreadPoolSize,
					"maxThreadPoolSize < coreThreadPoolSize");

			logger.info("Starting ThreadPoolExecutor for {}", this);
			logger.info("coreSize: {}", coreThreadPoolSize);
			logger.info("maxSize : {}", maxThreadPoolSize);

			ThreadPoolExecutor localExecutor = new ThreadPoolExecutor(coreThreadPoolSize, maxThreadPoolSize, time,
					timeUnit,
					queue);
			ThreadFactoryBuilder tfb = new ThreadFactoryBuilder().setDaemon(true).setNameFormat(name);
			if (uncaughtExceptionHandler != null) {
				tfb.setUncaughtExceptionHandler(uncaughtExceptionHandler);
			}
			localExecutor.setThreadFactory(tfb.build());

			localExecutor.setRejectedExecutionHandler(rejectedExecutionHandler);

			logger.info("ThreadPoolExecutor started");
			this.executor = localExecutor;
		}

	}

	@Override
	public void onSubscribe(Disposable s) {
		logger.info("onSubscribe({})", s);
		start();
	}

	@Override
	public void onNext(T t) {

		Runnable r = new Runnable() {

			@Override
			public void run() {

				publishSubject.onNext(t);
			}

		};

	
		executor.submit(r);
		

	}

	@Override
	public void onError(Throwable t) {
		logger.warn("onError()", t);

	}

	@Override
	public void onComplete() {
		logger.debug("onComplete()");

	}

	private void assertNotStarted() {
		Preconditions.checkState(executor == null, "start() called already");
	}

}
