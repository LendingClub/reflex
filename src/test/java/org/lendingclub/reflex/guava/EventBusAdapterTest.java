package org.lendingclub.reflex.guava;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.assertj.core.util.Lists;
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

	@Test
	public void test() throws InterruptedException {
		EventBus bus = new AsyncEventBus(Executors.newCachedThreadPool());

		List<String> stringList = Lists.newArrayList();

		EventBusAdapter emitter = EventBusAdapter.createAdapter(bus, Object.class);

		
		
		WorkQueue<Object> qs = new WorkQueue<>();
		emitter.getObservable().subscribe(qs);
		
		Disposable x = qs.getObservable().subscribe(c-> {
			logger.info("foo "+c);
			Thread.sleep(200);
			System.out.println(qs.getThreadPoolExecutor().toString());
		});

		for (int i=0; i<100;i++) {
		bus.post("test");
		bus.post(11);
		}

		Thread.sleep(10000L);
	}

	@Test
	public void test3() throws Exception {
		Scheduler s = Schedulers.trampoline();

		PublishSubject x = PublishSubject.create();
	
		x.observeOn(s).subscribe(it -> {
			logger.info("c {}", it);
			Thread.sleep(20);
		});
		x.observeOn(s).subscribe(it -> {
			logger.info("a {}", it);
			Thread.sleep(10);
		});
		x.observeOn(s).subscribe(it -> {
			logger.info("b {}", it);
			Thread.sleep(20);
		});
		

		for (int i = 0; i < 50; i++) {
			x.onNext("x "+i);
	
		
		}
		
		
		Thread.sleep(1000L);
	}
	
	@Test
	public void testSimple() {
		EventBus eventBus = new EventBus();
		
		Observable<Object> observable = EventBusAdapter.toObservable(eventBus, String.class);
		
		observable.subscribe(it -> {
			System.out.println("Hello, "+it);
		});
		
		eventBus.post("world");
		eventBus.post(123);
	}
	

}
