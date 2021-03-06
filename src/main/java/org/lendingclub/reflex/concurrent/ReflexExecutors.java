/**
 * Copyright 2017 Lending Club, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lendingclub.reflex.concurrent;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.reactivex.functions.Consumer;

public class ReflexExecutors {

	private static AtomicInteger instanceCount = new AtomicInteger(0);

	public static class ThreadPoolExecutorBuilder {
		public static int DEFAULT_THREAD_POOL_SIZE = 1;
		public static int DEFAULT_QUEUE_SIZE = 4096;
		public static int DEFAULT_KEEP_ALIVE_TIMEOUT_SECS = 30;

		int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
		
		int maxQueueSize = DEFAULT_QUEUE_SIZE;

		int keepAliveTime = DEFAULT_KEEP_ALIVE_TIMEOUT_SECS;
		TimeUnit keepAliveTimeUnit = TimeUnit.SECONDS;

		boolean allowCoreThreadTimeout = true;
	
	
		RejectedExecutionHandler rejectedExecutionHandler;
	
		ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
		
		protected ThreadPoolExecutorBuilder() {
			threadFactoryBuilder.setNameFormat("ReflexExecutor-" + instanceCount.incrementAndGet() + "-%d");

		}

		public ThreadPoolExecutorBuilder withThreadPoolSize(int count) {
			Preconditions.checkArgument(count>0,"thread pool size must be >0");
			this.threadPoolSize = count;
			return this;
		}

		public ThreadPoolExecutorBuilder withMaxQueueSize(int count) {
			this.maxQueueSize = count;
			return this;
		}

		public ThreadPoolExecutorBuilder withThreadTimeout(boolean b) {
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


			

			ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
					threadPoolSize,
					threadPoolSize,
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
