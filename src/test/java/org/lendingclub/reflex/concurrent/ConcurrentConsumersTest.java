package org.lendingclub.reflex.concurrent;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.operators.observable.ObservableSubscribeOn;
import io.reactivex.schedulers.Schedulers;

public class ConcurrentConsumersTest {

	@Test
	public void testExecutor() throws Exception {

		Logger logger = LoggerFactory.getLogger(getClass());
		Random r = new Random();
		int count = 100;
		CountDownLatch latch = new CountDownLatch(count);
		Consumer<Integer> c = new Consumer<Integer>() {

			@Override
			public void accept(Integer t) throws Exception {
				long x = r.nextInt(1000);
				logger.info("start... ({})", x);

				Thread.sleep(x);
				logger.info("complete");
				latch.countDown();
			}

		};

		Executor x = Executors.newFixedThreadPool(5);

		ConcurrentConsumers.subscribeParallel(Observable.range(0, count), x,c);

		latch.await(30000, TimeUnit.SECONDS);
		
		
	
	}

	@Test
	public void testSimple() {
		ConcurrentConsumers.subscribeParallel(
				Observable.range(0, 5),
				Schedulers.newThread(),
				val -> {
					System.out.println("processing "+val+" in "+Thread.currentThread());
				}
			);	
	}

}
