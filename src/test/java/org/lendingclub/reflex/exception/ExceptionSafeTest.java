package org.lendingclub.reflex.exception;

import javax.management.RuntimeErrorException;

import org.junit.Test;
import org.lendingclub.reflex.exception.ExceptionHandlers;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class ExceptionSafeTest {

	@Test
	public void testIt() {
		
		
		Observer<Integer> x = new Observer<Integer>() {

			@Override
			public void onSubscribe(Disposable d) {
				
				
			}

			@Override
			public void onNext(Integer t) {
			
				if (t==2) {
					throw new RuntimeException("simulated");
				}
				System.out.println("onNext("+t+")");
				
			}

			@Override
			public void onError(Throwable e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onComplete() {
				// TODO Auto-generated method stub
				
			}
		};
		
		Observable.just(1,2,3).subscribe(ExceptionHandlers.safeObserver(x));
	}

}
