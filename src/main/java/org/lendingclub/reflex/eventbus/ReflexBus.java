package org.lendingclub.reflex.eventbus;

import java.util.concurrent.Executor;

import org.lendingclub.reflex.concurrent.ConcurrentSubscribers;
import org.lendingclub.reflex.concurrent.ConcurrentSubscribers.ConcurrentSubscriber;
import org.lendingclub.reflex.concurrent.ReflexExecutors;
import org.lendingclub.reflex.concurrent.ReflexExecutors.ThreadPoolExecutorBuilder;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class ReflexBus {

	AsyncEventBus eventBus;
	Executor executor;

	protected ReflexBus() {
		// TODO Auto-generated constructor stub
	}


	public static class Builder {

		AsyncEventBus builderEventBus;
		Executor builderExecutor;

		ThreadPoolExecutorBuilder threadPoolExecutorBuilder = ReflexExecutors.newThreadPoolExecutorBuilder();


		protected Builder() {
			
		}
		public Builder withExecutor(Executor executor) {
			this.builderExecutor = executor;
			return this;
		}

		public Builder withThreadPoolExecutorConfig(Consumer<ThreadPoolExecutorBuilder> config) {
			try {
				config.accept(threadPoolExecutorBuilder);

				return this;
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}

		public ReflexBus build() {

			ReflexBus reflexBus = new ReflexBus();

			if (builderEventBus == null) {

				if (builderExecutor == null) {

					builderExecutor = threadPoolExecutorBuilder.build();
				}
				

				Preconditions.checkNotNull(builderExecutor);

				builderEventBus = new AsyncEventBus(builderExecutor);

				reflexBus.eventBus = builderEventBus;
				reflexBus.executor = builderExecutor;
				Preconditions.checkState(reflexBus.eventBus != null);
				// reflexBus.executor could be null...ok
			}

			return reflexBus;
		}

	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public Observable<Object> createObservable() {
		return createObservable(Object.class);
	}

	public <T> Observable<T> createObservable(Class<T> messageType) {

		return EventBusAdapter.toObservable(eventBus, messageType);

	}

	public <T> ConcurrentSubscriber<T> createConcurrentSubscriber(Class<T> messageType) {
		return ConcurrentSubscribers.createConcurrentSubscriber(createObservable(messageType));
	}

	public ConcurrentSubscriber<Object> createConcurrentSubscriber() {
		return createConcurrentSubscriber(Object.class);
	}
	
	public void post(Object obj) {
		eventBus.post(obj);
	}
	
	public Executor getExecutor() {
		return executor;
	}
	
	public EventBus getGuavaEventBus() {
		return eventBus;
	}
}
