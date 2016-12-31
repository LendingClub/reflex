package org.lendingclub.reflex.exception;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.lendingclub.reflex.exception.ExceptionHandlers;
import org.lendingclub.reflex.exception.ExceptionHandlers.UncaughtExceptionHandler;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class ExceptionHandlersTest {

	@Test
	public void testIt() {
		
	
		List<Integer> list = Lists.newCopyOnWriteArrayList();
		
		Observer<Integer> x = new Observer<Integer>() {

			@Override
			public void onSubscribe(Disposable d) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onNext(Integer t) {
				if (t==3) {
				throw new RuntimeException();
				}
				list.add(t);
				
			}

			@Override
			public void onError(Throwable e) {
				System.out.println(e);
				
			}

			@Override
			public void onComplete() {
				// TODO Auto-generated method stub
				
			}
		};
		
		list.clear();
		Observable.range(0, 5).subscribe(ExceptionHandlers.safeObserver(x));	
		Assertions.assertThat(list).containsExactly(0,1,2,4);
		
		list.clear();
		Observable.range(0, 5).subscribe(ExceptionHandlers.safeObserver(x, LoggerFactory.getLogger("foo")));	
		Assertions.assertThat(list).containsExactly(0,1,2,4);
		
		UncaughtExceptionHandler handler = new UncaughtExceptionHandler() {
			
	

			@Override
			public void handleException(Throwable t1, Object t2) {
				
			
				System.out.println(t1 +" "+t2);
			
				
			}
			
		};
		list.clear();
		Observable.range(0, 5).subscribe(ExceptionHandlers.safeObserver(x, handler));	
		Assertions.assertThat(list).containsExactly(0,1,2,4);
		
		
		UncaughtExceptionHandler brokenHandler = new UncaughtExceptionHandler() {
			
			

			@Override
			public void handleException(Throwable t1, Object t2) {
				
			
				throw new RuntimeException("broken handler!");
			
				
			}
			
		};
		list.clear();
		Observable.range(0, 5).subscribe(ExceptionHandlers.safeObserver(x, brokenHandler));	
		Assertions.assertThat(list).containsExactly(0,1,2,4);
	}

}
