package org.lendingclub.reflex.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ReflexExecutorsTest {

	@Test
	public void testUncaughtException() throws ExecutionException, InterruptedException, TimeoutException {

		// Con
		ThreadPoolExecutor tpe = ReflexExecutors.newThreadPoolExecutorBuilder()
				.withCorePoolSize(2)
				.withMaxPoolSize(2)
				.withKeepAliveTime(10, TimeUnit.SECONDS)
				.withMaxQueueSize(100)
				.withThreadNameFormat("Test-%d")
				.build();
				
		

		Runnable r = new Runnable() {

			@Override
			public void run() {

				throw new RuntimeException("simulated");

			}

		};

		for (int i = 0; i < 10; i++) {
			try {
				tpe.submit(r).get(20, TimeUnit.SECONDS);
				Assertions.fail("should have thrown exception");
			} catch (ExecutionException e) {
				Assertions.assertThat(e.getCause()).hasMessage("simulated");
			}
		}

	}
}
