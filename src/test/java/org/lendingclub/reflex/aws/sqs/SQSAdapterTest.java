/**
 * Copyright 2017 Lending Club, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lendingclub.reflex.aws.sqs;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.Observable;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.lendingclub.reflex.aws.sqs.SQSAdapter.SQSMessage;
import org.mockito.Mockito;

public class SQSAdapterTest {

  @Test
  public void testDefaults() {
    SQSAdapter adapter = new SQSAdapter();
    Assertions.assertThat(adapter.isAutoDeleteEnabled()).isTrue();
    Assertions.assertThat(adapter.backoffMaxMillis).isEqualTo(60000);
  }

  @Test
  public void testMockQueueLookup() {
    AmazonSQSClient client = Mockito.mock(AmazonSQSClient.class);

    GetQueueUrlResult result = new GetQueueUrlResult();
    result.setQueueUrl("https://sqs.us-west-2.amazonaws.com/123456789012/foo");
    Mockito.when(client.getQueueUrl("foo")).thenReturn(result);
    SQSAdapter adapter = new SQSAdapter().withQueueName("foo").withSQSClient(client);

    Assertions.assertThat(adapter.getQueueUrl()).isEqualTo(result.getQueueUrl());
  }

  @Test
  public void testLazyFailure() throws InterruptedException {

    AWSStaticCredentialsProvider cp =
        new AWSStaticCredentialsProvider(new BasicAWSCredentials("foo", "bar"));
    AmazonSQSClient client =
        (AmazonSQSClient)
            AmazonSQSClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .withCredentials(cp)
                .build();

    SQSAdapter adapter = new SQSAdapter().withQueueName("foo").withSQSClient(client);

    adapter.start();
    Thread.sleep(10000);
    // What we are checking for is that the adapters started running *AND*
    // continued to run
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

  @Test
  public void testOtherTypes() {
    ObjectMapper mapper = new ObjectMapper();

    SQSAdapter adapter = new SQSAdapter();
    SQSMessage m = adapter.new SQSMessage();
    Message msg = new Message();
    m.message = msg;
    msg.setBody(new ObjectMapper().createObjectNode().put("a", "1").toString());

    SQSMessage m1 = adapter.new SQSMessage();
    m1.message = new Message();
    ObjectNode innerMessage = mapper.createObjectNode().put("fizz", "buzz");
    m1.message.setBody(
        new ObjectMapper()
            .createObjectNode()
            .put("Type", "Notification")
            .put("Message", innerMessage.toString())
            .toString());

    List<JsonNode> x =
        Observable.just("foo", m, "bar", m1)
            .flatMap(new SQSAdapter.SQSJsonMessageExtractor())
            .toList()
            .blockingGet();

    Assertions.assertThat(x.get(0).path("a").asText()).isEqualTo("1");
    Assertions.assertThat(x.get(1).path("fizz").asText()).isEqualTo("buzz");
  }

  @Test
  public void testInitialRunState() {
    SQSAdapter adapter = new SQSAdapter();

    Assertions.assertThat(adapter.runStateRef.get().get()).isTrue();
    Assertions.assertThat(adapter.isRunning()).isFalse();
    Assertions.assertThat(adapter.running.get()).isFalse();
  }
}
