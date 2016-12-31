package org.lendingclub.reflex.aws.sqs;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.codahale.metrics.MetricRegistry;

public class SQSAdapterTest {



	@Test
	public void testDefaults() {
		SQSAdapter adapter = new SQSAdapter();
		Assertions.assertThat(adapter.isAutoDeleteEnabled()).isTrue();
		Assertions.assertThat(adapter.backoffMaxMillis).isEqualTo(60000);
	}
	
	@Test
	public void testMockQueueLookup()  {
		AmazonSQSClient client = Mockito.mock(AmazonSQSClient.class);
		
		GetQueueUrlResult result = new GetQueueUrlResult();
		result.setQueueUrl("https://sqs.us-west-2.amazonaws.com/123456789012/foo");
		Mockito.when(client.getQueueUrl("foo")).thenReturn(result);
		SQSAdapter adapter = new SQSAdapter()
				.withQueueName("foo")
				.withSQSClient(client);
		
		Assertions.assertThat(adapter.getQueueUrl()).isEqualTo(result.getQueueUrl());
	
	}
	
	@Test
	public void testLazyFailure()  throws InterruptedException {
	
		
		AmazonSQSClient client = new AmazonSQSClient(new BasicAWSCredentials("foo", "bar"));
		
		SQSAdapter adapter = new SQSAdapter()
				.withQueueName("foo")
				.withSQSClient(client);
		
		adapter.start();		
		Thread.sleep(10000);
		// What we are checking for is that the adapters started running *AND* continued to run
		Assertions.assertThat(adapter.successiveFailureCount.get()).isGreaterThanOrEqualTo(2);
		
		adapter.stop();
	}
	
	@Test
	public void testBackoff() {
		SQSAdapter adpter = new SQSAdapter();
		Assertions.assertThat(adpter.successiveFailureCount.get()).isEqualTo(0);
		Assertions.assertThat(adpter.getBackoffInterval()).isEqualTo(adpter.backoffMutiplierMillis);
		adpter.successiveFailureCount.set(5);
		Assertions.assertThat(adpter.getBackoffInterval()).isEqualTo(3200);
		
		adpter.successiveFailureCount.set(50);
		Assertions.assertThat(adpter.getBackoffInterval()).isEqualTo(adpter.backoffMaxMillis);
	}

}
