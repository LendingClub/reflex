package org.lendingclub.reflex.guava;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Test;
import org.lendingclub.reflex.guava.EventBusAdapter;
import org.lendingclub.reflex.predicate.Predicates;
import org.lendingclub.reflex.queue.WorkQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class EventBusAdapterTest {

	Logger logger = LoggerFactory.getLogger(EventBusAdapterTest.class);

	ExecutorService executor;
	
	@After
	public void cleanup() {
		if (executor!=null) {
			executor.shutdown();
			executor = null;
		}
	}
	@Test
	public void test() throws InterruptedException {
		int count = 10;
		executor = Executors.newFixedThreadPool(10);
		
		EventBus bus = new AsyncEventBus(executor);

		List<String> stringList = Lists.newArrayList();

		EventBusAdapter emitter = EventBusAdapter.createAdapter(bus, Object.class);

		WorkQueue<Object> qs = new WorkQueue<>();

		emitter.getObservable().subscribe(qs);

		AtomicReference<Thread> workerThread = new AtomicReference<Thread>(null);
		CountDownLatch latch = new CountDownLatch(count*2);
		Disposable x = qs.getObservable().subscribe(c -> {
			logger.info("foo " + c);
			Thread.sleep(300);
			latch.countDown();
			if (workerThread.get()!=null) {
				Assertions.assertThat(Thread.currentThread()).isSameAs(workerThread.get());
			}
			else {
				workerThread.set(Thread.currentThread());
			}
		});

		long t0 = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			bus.post("A "+i);
			bus.post("B "+i);
		}
		long t1 = System.currentTimeMillis();
		
		Assertions.assertThat(t1-t0).isLessThan(100);

		Assertions.assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
		
		long t2 = System.currentTimeMillis();
		
		// This verifies that we are using a single-threaded work queue.
		Assertions.assertThat(t2-t0).isGreaterThan(5000);
	}



	@Test
	public void testSimple() {
		EventBus eventBus = new EventBus();

		Observable<Object> observable = EventBusAdapter.toObservable(eventBus, String.class);

		observable.subscribe(it -> {
			logger.info("Hello, {}", it);
		});

		eventBus.post("world");
		eventBus.post(123);
	}

	/**
	 * This tests that @AllowCurrentEvents is enabled on the @Subscribe method
	 * of the EventBusAdapter. If it is NOT enabled, the Guava EventBus will
	 * serialize dispatching. This test verifies that this is not the case by
	 * timing the execution. If serialized, the total time process all
	 * observable subscriptions will far exceed a known limit.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testConcurrentEvents() throws InterruptedException {
		ExecutorService x = Executors.newFixedThreadPool(5);
		try {
			EventBus eventBus = new AsyncEventBus(x);
			Observable<Object> observable = EventBusAdapter.toObservable(eventBus, String.class);

			int count = 10;
			CountDownLatch latch = new CountDownLatch(count);
			observable.subscribe(it -> {
				logger.info("subscriber " + it);
				latch.countDown();
				Thread.sleep(500);
			});
			long t0 = System.currentTimeMillis();
			logger.info("start");
			for (int i = 0; i < count; i++) {
				eventBus.post("test " + i);
			}
			Assertions.assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue();
			logger.info("stop");
			long t1 = System.currentTimeMillis();

			Assertions.assertThat(t1 - t0).isLessThan(3000);
		} finally {
			x.shutdown();
		}
	}

}
