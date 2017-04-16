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
package org.lendingclub.reflex.metrics;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.lendingclub.reflex.metrics.ReflexMetrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;

public class ReflexMetricsTest {

	ThreadPoolExecutor tpe;
	
	@Test
	public void testIt() throws InterruptedException {

		MetricRegistry r = new MetricRegistry();

		ConsoleReporter.forRegistry(r).build().start(1, TimeUnit.SECONDS);

		LinkedBlockingDeque<Runnable> queue = new LinkedBlockingDeque<>(1024);
		System.out.println(queue.remainingCapacity());
		tpe = new ThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS, queue);

		new ReflexMetrics().withMetricRegistry(r).withPrefix("test").monitor(tpe, "testThreadPool");

		Assertions.assertThat(r.getMetrics().get("test.testThreadPool.corePoolSize")).isNotNull().isInstanceOf(
				Gauge.class);
		Runnable xx = new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(200);
				} catch (Exception e) {
				}

			}
		};

		int count = 100;
		for (int i = 0; i < count; i++) {
			tpe.execute(xx);
		}

		Thread.sleep(1000);
		Gauge<Long> g = (Gauge<Long>)  r.getMetrics().get("test.testThreadPool.completedTaskCount");
	
		long t0 = System.currentTimeMillis();
		boolean ok = false;
		for (int i=0; (System.currentTimeMillis()-t0 < 30000 )&& !ok; i++) {
			if (g.getValue().longValue()>=count) {
				ok = true;
			} 
			Thread.sleep(500);
			System.out.println("waiting ...");
		}
		Assertions.assertThat(ok).isTrue();

	}

	@After
	public void shutdown() {
		if (tpe!=null) {
			tpe.shutdown();
		}
	}
}
