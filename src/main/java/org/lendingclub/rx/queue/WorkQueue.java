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

import com.google.common.base.Preconditions;
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
public class WorkQueue<T> implements Observer<T> {

	Logger logger = LoggerFactory.getLogger(WorkQueue.class);

	static AtomicInteger count = new AtomicInteger(0);
	
	String name = "QueueSubscriber-"+(count.getAndIncrement())+"-%d";
	int queueSize = 1000;
	int coreThreadPoolSize = 1;
	int maxThreadPoolSize = 1;
	int time = 30;
	TimeUnit timeUnit = TimeUnit.SECONDS;
	RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.DiscardPolicy();
	LinkedBlockingDeque<Runnable> queue;
	ThreadPoolExecutor executor;
	
	PublishSubject<T> publishSubject = PublishSubject.create();
	
	
	public WorkQueue() {
		
	}

	public ThreadPoolExecutor getThreadPoolExecutor() {
		Preconditions.checkState(executor!=null,"WorkQueueObserver has not been started");
		return executor;
	}
	
	public WorkQueue<T> withQueueSize(int size) {
		this.queueSize = size;
		return this;
	}
	
	public WorkQueue<T> withCoreThreadPoolSize(int size) {
		this.coreThreadPoolSize = size;
		this.maxThreadPoolSize = Math.max(maxThreadPoolSize, this.coreThreadPoolSize);
		return this;
	}
	public WorkQueue<T> withRejectedExecutionHandler(RejectedExecutionHandler handler) {
		this.rejectedExecutionHandler = handler;
		return this;
	}
	public WorkQueue<T> withMaxThreadPoolSize(int size) {
		this.coreThreadPoolSize = size;
		return this;
	}
	public WorkQueue<T> withThreadTimeout(int time, TimeUnit timeUnit) {
		this.time = time;
		this.timeUnit = timeUnit;
		return this;
	}
	
	public WorkQueue<T> withThreadName(String name) {
		this.name = name;
		return this;
	}
	
	public Observable<T> getObservable() {
		return publishSubject;
	}
	
	protected synchronized void start() {
		if (executor == null) {
			queue = new LinkedBlockingDeque<>(queueSize);
			
			if (maxThreadPoolSize<coreThreadPoolSize) {
				throw new IllegalArgumentException("maxThreadPoolSize < coreThreadPoolSize");
			}
			logger.info("coreSize: {}",coreThreadPoolSize);
			logger.info("maxSize : {}",maxThreadPoolSize);
			executor= new ThreadPoolExecutor(coreThreadPoolSize,maxThreadPoolSize, time, timeUnit,
					queue);
			executor.setThreadFactory(new ThreadFactoryBuilder().setDaemon(true).setNameFormat(name).build());
			
			executor.setRejectedExecutionHandler(rejectedExecutionHandler);
			
			
		}

	}

	@Override
	public void onSubscribe(Disposable s) {
		logger.info("onSubscribe({})" , s);
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
		logger.info("onError()",t);

	}

	@Override
	public void onComplete() {
		logger.info("onComplete()");

	}

}
