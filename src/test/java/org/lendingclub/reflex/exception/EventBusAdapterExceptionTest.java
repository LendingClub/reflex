package org.lendingclub.reflex.exception;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.lendingclub.reflex.guava.EventBusAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.reactivex.Observable;

public class EventBusAdapterExceptionTest {

	Logger logger = LoggerFactory.getLogger(EventBusAdapter.class);
	ThreadPoolExecutor tpe;
	@Test
	public void testIt() throws InterruptedException {
		LinkedBlockingDeque<Runnable> q = new LinkedBlockingDeque<>(500);
		ThreadFactory tf = new ThreadFactoryBuilder().build();
		tpe = new ThreadPoolExecutor(2, 2, 10, TimeUnit.SECONDS, q, tf);
		int count=100;
		
		AsyncEventBus bus = new AsyncEventBus(tpe);
		
		Observable<Object> x = EventBusAdapter.toObservable(bus);
		
		CountDownLatch latch = new CountDownLatch(count);
		AtomicInteger totalCount = new AtomicInteger();
		AtomicInteger successCount = new AtomicInteger(0);
		
		x.subscribe(c -> {
			// This subscriber is going to blow up its monad/subscription.  That is just how reactive-streams work.
			logger.info("problem subscriber: {}",x);
			throw new RuntimeException();
		});
		
		x.subscribe(z->{
			
			// This subscription should *NOT FAIL*....the fact that the other subscriber blew up should not impact us here.
			latch.countDown();
			Thread.sleep(50);;
				
	
			logger.info("successful subscriber: {}",z);
		});
		
		for (int i=0; i<count; i++) {
			bus.post("message "+Integer.toString(i));
		}
		
		Assertions.assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
		
	}

	@After
	public void cleanup() {
		if (tpe!=null) {
			tpe.shutdown();
		}
	}
}
