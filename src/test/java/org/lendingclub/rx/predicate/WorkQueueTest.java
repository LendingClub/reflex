package org.lendingclub.rx.predicate;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.lendingclub.rx.queue.WorkQueueObserver;

import io.reactivex.Observable;

public class WorkQueueTest {

	@Test
	public void testIt() {
		
		WorkQueueObserver<Integer> queue = new WorkQueueObserver<Integer>();
		
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

}
