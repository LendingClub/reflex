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
				.withThreadPoolSize(2)
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

	@Test
	public void testCoreThreadTimeout() {
		ThreadPoolExecutor x = ReflexExecutors
				.newThreadPoolExecutorBuilder().withThreadTimeout(true).withKeepAliveTime(5, TimeUnit.SECONDS)
				.withThreadPoolSize(10).build();

		x.submit(() -> {
		});

		boolean conditionSatisfied = false;
		long t0 = System.currentTimeMillis();
		while ((!conditionSatisfied) && System.currentTimeMillis() - t0 < 60000) {
			if (x.getPoolSize() == 0) {
				conditionSatisfied = true;
			}
			try {
				Thread.sleep(300);
			} catch (Exception e) {
			}
		}
		System.out.println(System.currentTimeMillis() - t0);
		Assertions.assertThat(conditionSatisfied).isTrue();
	}
}
