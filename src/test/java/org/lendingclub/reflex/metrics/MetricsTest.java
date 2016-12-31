package org.lendingclub.reflex.metrics;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.lendingclub.reflex.metrics.MetricsMonitor;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;

public class MetricsTest {

	ThreadPoolExecutor tpe;
	
	@Test
	public void testIt() throws InterruptedException {

		MetricRegistry r = new MetricRegistry();

		ConsoleReporter.forRegistry(r).build().start(1, TimeUnit.SECONDS);

		LinkedBlockingDeque<Runnable> queue = new LinkedBlockingDeque<>(1024);
		System.out.println(queue.remainingCapacity());
		tpe = new ThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS, queue);

		new MetricsMonitor().withMetricRegistry(r).withPrefix("test").monitor(tpe, "testThreadPool");

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
