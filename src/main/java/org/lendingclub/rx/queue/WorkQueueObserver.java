package org.lendingclub.rx.queue;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

/**
 * WorkQueueObserver connects an Rx Observable to a bounded work queue and exposes the resulting work queue as an Observable.
 * @author rschoening
 *
 * @param <T>
 */
public class WorkQueueObserver<T> implements Observer<T> {

	Logger logger = LoggerFactory.getLogger(WorkQueueObserver.class);

	static AtomicInteger count = new AtomicInteger(0);
	
	String name = "QueueSubscriber-"+(count.getAndIncrement())+"-%d";
	int queueSize = 1000;
	int coreThreadPoolSize = 5;
	int maxThreadPoolSize = 10;
	int time = 30;
	TimeUnit timeUnit = TimeUnit.SECONDS;
	RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.DiscardOldestPolicy();
	LinkedBlockingDeque<Runnable> queue;
	ThreadPoolExecutor executor;
	
	PublishSubject<T> publishSubject = PublishSubject.create();
	
	
	public WorkQueueObserver() {
		
	}

	public ThreadPoolExecutor getThreadPoolExecutor() {
		return executor;
	}
	
	public WorkQueueObserver<T> withQueueSize(int size) {
		this.queueSize = size;
		return this;
	}
	
	public WorkQueueObserver<T> withCoreThreadPoolSize(int size) {
		this.coreThreadPoolSize = size;
		return this;
	}
	public WorkQueueObserver<T> withRejectedExecutionHandler(RejectedExecutionHandler handler) {
		this.rejectedExecutionHandler = handler;
		return this;
	}
	public WorkQueueObserver<T> withMaxThreadPoolSize(int size) {
		this.coreThreadPoolSize = size;
		return this;
	}
	public WorkQueueObserver<T> withThreadTimeout(int time, TimeUnit timeUnit) {
		this.time = time;
		this.timeUnit = timeUnit;
		return this;
	}
	
	public WorkQueueObserver<T> withThreadName(String name) {
		this.name = name;
		return this;
	}
	
	public Observable<T> getObservable() {
		return publishSubject;
	}
	
	protected synchronized void start() {
		if (executor == null) {
			queue = new LinkedBlockingDeque<>(queueSize);
			
		
			executor= new ThreadPoolExecutor(coreThreadPoolSize,maxThreadPoolSize, time, timeUnit,
					queue);
			executor.setThreadFactory(new ThreadFactoryBuilder().setDaemon(true).setNameFormat(name).build());
			
			executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
			
			
		}

	}

	@Override
	public void onSubscribe(Disposable s) {
		logger.info("onSubscribe " + s);
		start();
	}

	@Override
	public void onNext(T t) {
		
		Runnable r = new Runnable() {

			@Override
			public void run() {
				logger.debug("onNext() "+t);
				publishSubject.onNext(t);
			}
			
		};
		
		executor.submit(r);

	}

	@Override
	public void onError(Throwable t) {
		logger.info("onError()",t);

	}

	@Override
	public void onComplete() {
		logger.info("onComplete()");

	}

}
