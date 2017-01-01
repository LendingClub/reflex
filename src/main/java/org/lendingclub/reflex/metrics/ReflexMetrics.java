package org.lendingclub.reflex.metrics;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

import org.lendingclub.reflex.aws.sqs.SQSAdapter;
import org.lendingclub.reflex.guava.EventBusAdapter;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class ReflexMetrics {

	MetricRegistry registry;
	String prefix;

	public ReflexMetrics withMetricRegistry(MetricRegistry r) {

		ReflexMetrics m = new ReflexMetrics();
		m.registry = r;

		return m;
	}

	public ReflexMetrics withPrefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	public String name(String localName, String attribute) {

		StringBuffer sb = new StringBuffer();
		if (!Strings.isNullOrEmpty(prefix)) {
			sb.append(prefix);
			sb.append(".");
		}
		sb.append(localName);
		sb.append(".");
		sb.append(attribute);
		return sb.toString();

	}

	public ReflexMetrics monitor(ThreadPoolExecutor tpe, String name) {
		
		Preconditions.checkState(registry!=null,"MetricRegistry not set.  Please call withMetricRegistry() first");
		Gauge<Integer> activeCountGauge = new Gauge<Integer>() {

			@Override
			public Integer getValue() {
				return tpe.getActiveCount();
			}
		};
		registry.register(name(name, "activeCount"), activeCountGauge);

		Gauge<Long> completedTaskCountGauge = new Gauge<Long>() {

			@Override
			public Long getValue() {
				return tpe.getCompletedTaskCount();
			}
		};
		registry.register(name(name, "completedTaskCount"), completedTaskCountGauge);

		Gauge<Integer> corePoolSize = new Gauge<Integer>() {

			@Override
			public Integer getValue() {
				return tpe.getCorePoolSize();
			}
		};
		registry.register(name(name, "corePoolSize"), corePoolSize);

		Gauge<Integer> largestPoolSize = new Gauge<Integer>() {

			@Override
			public Integer getValue() {
				return tpe.getLargestPoolSize();
			}
		};
		registry.register(name(name, "largestPoolSize"), largestPoolSize);
		Gauge<Integer> maxPoolSize = new Gauge<Integer>() {

			@Override
			public Integer getValue() {
				return tpe.getMaximumPoolSize();
			}
		};
		registry.register(name(name, "maxPoolSize"), maxPoolSize);

		Gauge<Long> taskCount = new Gauge<Long>() {

			@Override
			public Long getValue() {
				return tpe.getTaskCount();
			}
		};
		registry.register(name(name, "taskCount"), taskCount);

		Gauge<Integer> queueRemainingCapcity = new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return tpe.getQueue().remainingCapacity();
			}
		};
		registry.register(name(name, "queueRemainingCapacity"), queueRemainingCapcity);

		registry.register(name(name, "queueSize"), new Gauge<Integer>() {
			@Override
			public Integer getValue() {

				return tpe.getQueue().size();
			}
		});

		return this;
	}

	
	protected Gauge registerGauge(String name, String attribute,  AtomicLong l) {
		Gauge g = gauge(l);
		registry.register(name(name, attribute), g);
		return g;
	}

	protected Gauge<Long> gauge(final AtomicLong data) {
		return new Gauge<Long>() {

			@Override
			public Long getValue() {
				return data.get();
			}

		};
	}


	public ReflexMetrics monitor(SQSAdapter adapter, String name) {

		registerGauge(name,"failure", adapter.getSuccesiveFailureCount());
		registerGauge(name,"totalFailure",
				adapter.getTotalFailureCount());
		
		registerGauge(name,"messagesReceived",adapter.getTotalMessagesReceivedCount());
		registerGauge(name,"success",adapter.getTotalSuccessCount());
		return this;
	}
	
	public ReflexMetrics monitor(EventBusAdapter adapter, String name) {
		
		registerGauge(name, "count",adapter.getMessageCount());
		return this;
	}
	


}
