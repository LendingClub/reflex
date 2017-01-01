package org.lendingclub.reflex.concurrent;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class ReflexExecutors {

	static AtomicInteger instanceCount = new AtomicInteger(0);
	public static class ThreadPoolExecutorBuilder {
		int corePoolSize = 1;
		int maxPoolSize = 1;
		int maxQueueSize = 1024;
		
		int keepAliveTime = 30;
		TimeUnit keepAliveTimeUnit = TimeUnit.SECONDS;
		
		String threadNameFormat = "ReflexExecutor-"+instanceCount.incrementAndGet()+"-%d";
		UncaughtExceptionHandler uncaughtExceptionHandler;
		RejectedExecutionHandler rejectedExecutionHandler;
		
		
		protected ThreadPoolExecutorBuilder() {
			
		}
		public ThreadPoolExecutorBuilder withCorePoolSize(int count) {
			this.corePoolSize = count;
			return this;
		}
		public ThreadPoolExecutorBuilder withMaxPoolSize(int count) {
			this.maxPoolSize = count;
			return this;
		}
		public ThreadPoolExecutorBuilder withMaxQueueSize(int count) {
			this.maxQueueSize = count;
			return this;
		}
		public ThreadPoolExecutorBuilder withKeepAliveTime(int time, TimeUnit unit) {
			this.keepAliveTime = time;
			this.keepAliveTimeUnit = unit;
			return this;
		}
		
		public ThreadPoolExecutorBuilder withUncaughtExceptionHandler(UncaughtExceptionHandler h) {
			this.uncaughtExceptionHandler = h;
			return this;
		}
		public ThreadPoolExecutorBuilder withRejectedExecutionHandler(RejectedExecutionHandler h) {
			this.rejectedExecutionHandler = h;
			return this;
		}
		
		public ThreadPoolExecutorBuilder withThreadNameFormat(String name) {
			this.threadNameFormat = name;
			return this;
		}
		public ThreadPoolExecutor build() {
			LinkedBlockingDeque<Runnable> queue = new LinkedBlockingDeque<>(maxQueueSize);
			
			ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
			if (threadNameFormat!=null) {
				tfb.setNameFormat(threadNameFormat);
			}
			if (uncaughtExceptionHandler!=null) {
				tfb.setUncaughtExceptionHandler(uncaughtExceptionHandler);
			}
			
			maxPoolSize = Math.max(maxPoolSize, corePoolSize);

			ThreadPoolExecutor tpe = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, keepAliveTimeUnit, queue,tfb.build());
			
			if (rejectedExecutionHandler!=null) {
				tpe.setRejectedExecutionHandler(rejectedExecutionHandler);
			}
			
			
			
			return tpe;
		}
	}
	
	public static ReflexExecutors.ThreadPoolExecutorBuilder newThreadPoolExecutorBuilder() {
		return new ThreadPoolExecutorBuilder();
	}

}
