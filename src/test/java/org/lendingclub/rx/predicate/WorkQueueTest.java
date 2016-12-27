package org.lendingclub.rx.predicate;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.lendingclub.rx.queue.WorkQueue;

import io.reactivex.Observable;

public class WorkQueueTest {

	@Test
	public void testIt() {
		
		WorkQueue<Integer> queue = new WorkQueue<Integer>();
		
		queue
			.withCoreThreadPoolSize(5)
			.withQueueSize(5000)
			.withThreadName("my-thread-%d")
			.withThreadTimeout(30, TimeUnit.SECONDS);
		
		queue.getObservable().subscribe(it -> {
			System.out.println("processing "+it+" in "+Thread.currentThread());
		});
		
		Observable.range(0, 100).subscribe(queue);
		
	
		
		
	}
	
	
	@Test
	public void testDefaults() {
		
		WorkQueue<Integer> queue = new WorkQueue<Integer>();
		
		
		queue.getObservable().subscribe(it -> {
			System.out.println("processing "+it+" in "+Thread.currentThread());
		});
		
		Observable.range(0, 100).subscribe(queue);
		
	
		Assertions.assertThat(queue.getThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);
		Assertions.assertThat(queue.getThreadPoolExecutor().getMaximumPoolSize()).isEqualTo(1);
		Assertions.assertThat(queue.getThreadPoolExecutor().getRejectedExecutionHandler()).isInstanceOf(ThreadPoolExecutor.DiscardPolicy.class);
	}

}
