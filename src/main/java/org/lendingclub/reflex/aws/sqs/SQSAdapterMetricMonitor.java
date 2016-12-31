package org.lendingclub.reflex.aws.sqs;

import org.lendingclub.reflex.metrics.MetricsMonitor;

public class SQSAdapterMetricMonitor  extends MetricsMonitor {

	public MetricsMonitor monitor(SQSAdapter adapter, String name) {

		registerGauge(name,"failure", adapter.getSuccesiveFailureCount());
		registerGauge(name,"totalFailure",
				adapter.getTotalFailureCount());
		
		registerGauge(name,"messagesReceived",adapter.getTotalMessagesReceivedCount());
		registerGauge(name,"success",adapter.getTotalSuccessCount());
		return this;
	}

}
