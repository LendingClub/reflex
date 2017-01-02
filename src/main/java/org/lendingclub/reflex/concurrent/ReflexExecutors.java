package org.lendingclub.reflex.concurrent;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.reactivex.functions.Consumer;

public class ReflexExecutors {

	private static AtomicInteger instanceCount = new AtomicInteger(0);

	public static class ThreadPoolExecutorBuilder {
		public static int DEFAULT_THREAD_POOL_SIZE = 1;
		public static int DEFAULT_QUEUE_SIZE = 4096;
		public static int DEFAULT_KEEP_ALIVE_TIMEOUT_SECS = 30;

		int corePoolSize = DEFAULT_THREAD_POOL_SIZE;
		int maxPoolSize = DEFAULT_THREAD_POOL_SIZE;
		int maxQueueSize = DEFAULT_QUEUE_SIZE;

		int keepAliveTime = DEFAULT_KEEP_ALIVE_TIMEOUT_SECS;
		TimeUnit keepAliveTimeUnit = TimeUnit.SECONDS;

		boolean allowCoreThreadTimeout = true;
	
	
		RejectedExecutionHandler rejectedExecutionHandler;
	
		ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
		
		protected ThreadPoolExecutorBuilder() {
			threadFactoryBuilder.setNameFormat("ReflexExecutor-" + instanceCount.incrementAndGet() + "-%d");

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

		public ThreadPoolExecutorBuilder withCoreThreadTimeout(boolean b) {
			this.allowCoreThreadTimeout = b;
			return this;
		}

		public ThreadPoolExecutorBuilder withKeepAliveTime(int time, TimeUnit unit) {
			this.keepAliveTime = time;
			this.keepAliveTimeUnit = unit;
			return this;
		}

		public ThreadPoolExecutorBuilder withUncaughtExceptionHandler(UncaughtExceptionHandler h) {
			this.threadFactoryBuilder.setUncaughtExceptionHandler(h);
			return this;
		}

		public ThreadPoolExecutorBuilder withRejectedExecutionHandler(RejectedExecutionHandler h) {
			this.rejectedExecutionHandler = h;
			return this;
		}

		public ThreadPoolExecutorBuilder withThreadNameFormat(String name) {
			this.threadFactoryBuilder.setNameFormat(name);
			return this;
		}

		public void withThreadFactoryConfig(Consumer<ThreadFactoryBuilder> config) {
			try {
				config.accept(threadFactoryBuilder);
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}
		public ThreadPoolExecutor build() {
			LinkedBlockingDeque<Runnable> queue = new LinkedBlockingDeque<>(maxQueueSize);


			maxPoolSize = Math.max(maxPoolSize, corePoolSize);

			ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
					corePoolSize,
					maxPoolSize,
					keepAliveTime,
					keepAliveTimeUnit,
					queue,
					threadFactoryBuilder.build());
			
			threadPoolExecutor.allowCoreThreadTimeOut(allowCoreThreadTimeout);
			if (rejectedExecutionHandler != null) {
				threadPoolExecutor.setRejectedExecutionHandler(rejectedExecutionHandler);
			}

			return threadPoolExecutor;
		}
	}

	public static ReflexExecutors.ThreadPoolExecutorBuilder newThreadPoolExecutorBuilder() {
		return new ThreadPoolExecutorBuilder();
	}

}
