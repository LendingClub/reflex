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

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.lendingclub.reflex.concurrent.ConcurrentSubscribers.ConcurrentSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.operators.observable.ObservableSubscribeOn;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class ConcurrentSubscribersTest {

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

		ConcurrentSubscribers.subscribeParallel(Observable.range(0, count), x,c);

		latch.await(30000, TimeUnit.SECONDS);
		
		
	
	}

	@Test
	public void testSimple() {
		ConcurrentSubscribers.subscribeParallel(
				Observable.range(0, 5),
				Schedulers.newThread(),
				val -> {
					System.out.println("processing "+val+" in "+Thread.currentThread());
				}
			);	
	}
	
	@Test
	public void testIt() {
		
		ConcurrentSubscribers.createConcurrentSubscriber(Observable.range(0, 10))
		.withScheduler(Schedulers.newThread())
		.subscribe(val -> {
			System.out.println("processing "+val+" in "+Thread.currentThread());
		});
		
	}
	
	@Test
	public void testBounded()throws InterruptedException  {
		
		int count = 100;
		CountDownLatch latch = new CountDownLatch( count);
		ConcurrentSubscriber<Integer> x = ConcurrentSubscribers.createConcurrentSubscriber(Observable.range(0, count))
		.withNewExecutor(b->{
			b.withThreadPoolSize(5)
			.withMaxQueueSize(1024);
		})
	
		.subscribe(val -> {
			System.out.println("processing "+val+" in "+Thread.currentThread());
			latch.countDown();
		});
		
		
		
		Assertions.assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
		
		ThreadPoolExecutor.class.cast(x.getExecutor().get()).shutdown();
		
	}
	
	@Test
	public void testParallelFailures() throws InterruptedException {
		
		// This tests that failures are contained
		int count = 100;
		Subject<Integer> p = PublishSubject.create();
		p = p.toSerialized();
		CountDownLatch latch = new CountDownLatch(count-10);
		ConcurrentSubscribers.createConcurrentSubscriber(p)
		.withNewExecutor(builder -> {
			builder.withThreadPoolSize(5);
		})
		
		.subscribe(val -> {
			System.out.println("processing "+val+" in "+Thread.currentThread());
			if (val%10==5) {
				throw new RuntimeException("simulated failure");
			}
			latch.countDown();
		});
		
		
		for (int i=0; i<100; i++) {
			p.onNext(i);
		}
		
		
		Assertions.assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
	}

}
