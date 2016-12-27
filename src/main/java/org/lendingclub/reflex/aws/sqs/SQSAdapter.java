package org.lendingclub.reflex.aws.sqs;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;

public class SQSAdapter {

	static ObjectMapper mapper = new ObjectMapper();

	public class SQSMessage {
		Message message;

		public Message getMessage() {
			return message;
		}

		public SQSAdapter getSQSAdapter() {
			return SQSAdapter.this;
		}
	}

	static Logger logger = LoggerFactory.getLogger(SQSAdapter.class);

	PublishSubject<SQSMessage> publishSubject = PublishSubject.create();
	String queueUrl;
	AmazonSQSClient sqs;
	String queueName;
	boolean autoDelete = true;
	int waitTimeSeconds = 10;
	int messagesPerRequest = 10;
	AtomicBoolean running = new AtomicBoolean(false);

	AtomicLong failureCount = new AtomicLong(0);

	public SQSAdapter withSQSClient(AmazonSQSClient client) {
		this.sqs = client;
		return this;
	}

	public SQSAdapter withWaitTimeSeconds(int secs) {
		this.waitTimeSeconds = secs;
		return this;
	}

	public SQSAdapter withMaxMessagesPerRequest(int count) {
		this.messagesPerRequest = count;
		return this;
	}

	public SQSAdapter withQueueName(String name) {
		this.queueName = name;
		return this;
	}

	public SQSAdapter withQueueUrl(String url) {
		this.queueUrl = url;
		return this;
	}

	public static class SQSJsonMessageExtractor implements Function<SQSMessage, Observable<JsonNode>> {

		@Override
		public Observable<JsonNode> apply(SQSMessage t) throws Exception {
			try {
				String data = t.getMessage().getBody();
				JsonNode n = mapper.readTree(data);
				if (n.path("Type").asText().equals("Notification")) {
					n = mapper.readTree(n.path("Message").asText());
				}
				return Observable.just(n);
			} catch (Exception e) {
				logger.warn("problem", e);
			}
			return Observable.empty();
		}

	}

	public synchronized SQSAdapter start() {
		if (running.get()) {
			throw new IllegalStateException("already running");
		}
		if (queueUrl == null && queueName != null) {
			GetQueueUrlResult getQueueUrlResult = this.sqs.getQueueUrl(queueName);
			queueUrl = getQueueUrlResult.getQueueUrl();
		}

		Runnable r = new Runnable() {

			public void run() {

				try {
					running.set(true);
					while (running.get()) {
						ReceiveMessageRequest rmr = new ReceiveMessageRequest();
						rmr.setWaitTimeSeconds(waitTimeSeconds);
						rmr.setMaxNumberOfMessages(messagesPerRequest);
						rmr.setQueueUrl(queueUrl);
						ReceiveMessageResult result = sqs.receiveMessage(rmr);
						List<Message> list = result.getMessages();

						if (list != null) {
							for (Message message : list) {
								try {
									logger.info("received: {}", message.getMessageId());
									SQSMessage sqs = new SQSMessage();
									sqs.message = message;
									publishSubject.onNext(sqs);
									if (autoDelete) {
										delete(message);
									}
									resetBackoff();
								} catch (Exception e) {
									handleException(e);
								}
							}
						}
					}
				} catch (Exception e) {
					handleException(e);
				}

			}
		};

		ThreadGroup tg = new ThreadGroup("SQSAdapter");
		Thread t = new Thread(tg, r);

		t.setDaemon(true);
		t.start();

		return this;
	}

	public void delete(Message m) {
		logger.info("deleting {} {}", queueUrl, m.getReceiptHandle());
		DeleteMessageRequest dmr = new DeleteMessageRequest(queueUrl, m.getReceiptHandle());
		sqs.deleteMessage(dmr);
	}

	private void resetBackoff() {
		failureCount.set(0);
	}

	protected void handleException(Exception e) {
		logger.warn("problem receiving message from queue: " + queueUrl, e);
		long count = failureCount.incrementAndGet();

		long wait = (count ^ 2) * 100;
		wait = Math.min(wait, TimeUnit.SECONDS.toMillis(60));
		try {
			Thread.sleep(wait);
		} catch (InterruptedException x) {
		}
	}

	public Observable<SQSMessage> getObservable() {
		return publishSubject;
	}
}
