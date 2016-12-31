package org.lendingclub.reflex.guava;

import org.lendingclub.reflex.metrics.MetricsMonitor;

public class EventBusAdapterMetricMonitor extends MetricsMonitor {


	public EventBusAdapterMetricMonitor monitor(EventBusAdapter adapter, String name) {
		
		registerGauge(name, "count",adapter.counter);
		return this;
	}
}
