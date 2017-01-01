package org.lendingclub.reflex.concurrent;

import java.util.concurrent.Executor;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class ConcurrentConsumers {


	public static <T> void subscribeParallel(Observable<T> x, Scheduler scheduler, Consumer<T> c) {
		x.flatMap(val->{
			Observable.just(val).subscribeOn(scheduler).subscribe(c);
			return Observable.just(val);
		}).subscribe(dummy ->{});
	}
	public static <T> void subscribeParallel(Observable<T> x, Executor executor, Consumer<T> c) {
		
		subscribeParallel(x, Schedulers.from(executor), c);
		
	}
	
	
	
}
