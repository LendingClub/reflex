package org.lendingclub.reflex.predicate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.lendingclub.reflex.consumer.Consumers;
import org.lendingclub.reflex.queue.WorkQueue;

import io.reactivex.Observable;

public class WorkQueueTest {

	@Test
	public void testIt() throws InterruptedException {

		int count = 100;
		CountDownLatch latch = new CountDownLatch(count);
		WorkQueue<Integer> queue = new WorkQueue<Integer>();

		queue
				.withCoreThreadPoolSize(5)
				.withQueueSize(5000)
				.withThreadName("my-thread-%d")
				.withThreadTimeout(30, TimeUnit.SECONDS);

		queue.getObservable().subscribe(it -> {
			System.out.println("processing " + it + " in " + Thread.currentThread());
			latch.countDown();
		});

		Observable.range(0, count).subscribe(queue);

		Assertions.assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		queue.getThreadPoolExecutor().shutdown();
	}

	@Test
	public void testIt2() throws InterruptedException {
		
		int count = 100;
		WorkQueue<Integer> queue = new WorkQueue<Integer>();

		CountDownLatch latch = new CountDownLatch(count);
		AtomicInteger totalCount = new AtomicInteger();
		AtomicInteger success = new AtomicInteger();
		queue
				.withCoreThreadPoolSize(5)
				.withQueueSize(5000)
				.withThreadName("my-thread-%d")
				.withThreadTimeout(30, TimeUnit.SECONDS);
				

		queue.getObservable().subscribe(Consumers.safeConsumer(it -> {
		
			if (totalCount.incrementAndGet() % 10==0) {
				latch.countDown();
				throw new RuntimeException();
			}
			System.out.println("processing " + it + " in " + Thread.currentThread());
			success.incrementAndGet();
			latch.countDown();
		}));

		Observable.range(0, count).subscribe(queue);

		Assertions.assertThat(latch.await(10,TimeUnit.SECONDS)).isTrue();
	
		Assertions.assertThat(success.get()).isEqualTo(90);
		queue.getThreadPoolExecutor().shutdown();
	}

	@Test
	public void testDefaults() throws InterruptedException {

		WorkQueue<Integer> queue = new WorkQueue<Integer>();
		int count = 100;
		CountDownLatch latch = new CountDownLatch(count);
		queue.getObservable().subscribe(it -> {
			System.out.println("processing " + it + " in " + Thread.currentThread());
			latch.countDown();
		});

		Observable.range(0, 100).subscribe(queue);

		Assertions.assertThat(queue.getThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);
		Assertions.assertThat(queue.getThreadPoolExecutor().getMaximumPoolSize()).isEqualTo(1);
		Assertions.assertThat(queue.getThreadPoolExecutor().getRejectedExecutionHandler()).isInstanceOf(
				ThreadPoolExecutor.DiscardPolicy.class);
		Assertions.assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		queue.getThreadPoolExecutor().shutdown();
	}

}
