package org.lendingclub.reflex.aws.sqs;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.UncaughtExceptionHandlers;

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

		public void delete() {
			SQSAdapter.this.delete(message);
		}
	}
	
	public static class SQSUrlSupplier implements Supplier<String> {

		private String queueUrl;
		private AmazonSQSClient client;
		private String queueName;

		public SQSUrlSupplier(String url) {
			this.queueUrl = url;
		}

		public SQSUrlSupplier(AmazonSQSClient client, String queueName) {
			this.client = client;
			this.queueName = queueName;
		}

		@Override
		public String get() {
			if (!Strings.isNullOrEmpty(queueUrl)) {
				return queueUrl;
			} else if (client != null && queueName != null) {
				logger.info("resolving url for queue: {}", queueName);

				GetQueueUrlResult result = client.getQueueUrl(queueName);

				return result.getQueueUrl();
			}
			throw new IllegalArgumentException("must provide explicit url or a client+queueName");
		}
	}

	static Logger logger = LoggerFactory.getLogger(SQSAdapter.class);

	PublishSubject<SQSMessage> publishSubject = PublishSubject.create();
	Supplier<String> urlSupplier;
	AmazonSQSClient sqs;
	String queueName;
	boolean autoDelete = true;
	int waitTimeSeconds = 10;
	int messagesPerRequest = 10;
	AtomicBoolean running = new AtomicBoolean(false);

	long backoffMutiplierMillis = 100;
	long backoffMaxMillis = TimeUnit.SECONDS.toMillis(60);
	AtomicLong failureCount = new AtomicLong(0);


	public SQSAdapter withSQSClient(AmazonSQSClient client) {

		this.sqs = client;
		return this;
	}

	/**
	 * SQS is implemented with long-polling. This is the number of seconds that
	 * the SQS receive-message operation will block before closing and executing
	 * another. Default: 10
	 * 
	 * @param secs
	 * @return
	 */
	public SQSAdapter withWaitTimeSeconds(int secs) {
		this.waitTimeSeconds = secs;
		return this;
	}

	/**
	 * The SQS client can request between 1-10 messages per GET request.
	 * Default: 10.
	 * 
	 * @param count
	 * @return
	 */
	public SQSAdapter withMaxMessagesPerRequest(int count) {
		this.messagesPerRequest = count;
		return this;
	}

	/**
	 * If you do not specify the queue URL and the client is set to the correct
	 * region, SQSAdapter will resolve the URL from the unqualified queue name.
	 * If the url is set, there is no need to set the name.
	 * 
	 * @param name
	 * @return
	 */
	public SQSAdapter withQueueName(String name) {
		this.queueName = name;
		return this;
	}

	/**
	 * Exponential backoff multiplier. (failureCount ^ 2 * multiplier)
	 * 
	 * @param millis
	 * @return
	 */
	public SQSAdapter withBackoffMultiplierMillis(long millis) {
		assertNotStarted();
		this.backoffMutiplierMillis = millis;
		return this;
	}

	public boolean isAutoDeleteEnabled() {
		return autoDelete;
	}

	public SQSAdapter withAutoDeleteEnabled(boolean b) {
		assertNotStarted();
		this.autoDelete = b;
		return this;
	}

	/**
	 * Maximum number of milliseconds that retries will wait in the case of
	 * failure. Default: 60000ms (60 seconds)
	 * 
	 * @param millis
	 * @return
	 */
	public SQSAdapter withBackofMaxMillis(long millis) {
		assertNotStarted();
		this.backoffMaxMillis = millis;
		return this;
	}

	/**
	 * Sets the SQS queue URL that the SQSAdpater will use for polling.
	 * 
	 * @param url
	 * @return
	 */
	public SQSAdapter withQueueUrl(String url) {
		assertNotStarted();
		this.urlSupplier = new SQSUrlSupplier(url);
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
				logger.warn("problem parsing message from queue: " + t.getSQSAdapter().getQueueUrl(), e);
			}
			return Observable.empty();
		}

	}

	public String getQueueUrl() {
		if (urlSupplier == null) {
			if ((!Strings.isNullOrEmpty(queueName)) && sqs != null) {
				urlSupplier = Suppliers.memoize(new SQSUrlSupplier(sqs, queueName));
			} else {
				return null;
			}
		}
		return urlSupplier.get();
	}

	public synchronized void stop() {
		logger.info("stopping....");
		running.set(false);
	}
	public synchronized SQSAdapter start() {
		if (running.get()) {
			logger.warn("already running");
			return this;
		}

		Preconditions.checkArgument(sqs != null, "SQSClient must be set");
		if (urlSupplier == null && queueName != null) {
			urlSupplier = Suppliers.memoize(new SQSUrlSupplier(sqs, queueName));
		}

		if (urlSupplier == null) {
			throw new IllegalArgumentException("queueUrl or queueName must be set");
		}

		Runnable r = new Runnable() {

			public void run() {

				running.set(true);
				while (running.get()) {
					try {
						ReceiveMessageRequest rmr = new ReceiveMessageRequest();
						rmr.setWaitTimeSeconds(waitTimeSeconds);
						rmr.setMaxNumberOfMessages(messagesPerRequest);
						if (urlSupplier == null) {
							throw new IllegalArgumentException("queueUrl or queueName must be set");
						}

						rmr.setQueueUrl(urlSupplier.get());
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
									resetFailureCount();
								} catch (Exception e) {
									handleException(e);
								}
							}
						}

					} catch (Exception e) {
						handleException(e);
					}
				}
				logger.info("stopped");
			}
		};

		ThreadGroup tg = new ThreadGroup("SQSAdapter");
		Thread t = new Thread(tg, r);

		t.setDaemon(true);
		t.start();

		return this;
	}

	public void delete(Message m) {
		String url = getQueueUrl();
		logger.info("deleting {} {}", url, m.getReceiptHandle());
		DeleteMessageRequest dmr = new DeleteMessageRequest(url, m.getReceiptHandle());
		sqs.deleteMessage(dmr);
	}

	private void resetFailureCount() {
		failureCount.set(0);
	}

	private void assertNotStarted() {
		Preconditions.checkState(running.get() == false, "start() already called");
	}

	protected void handleException(Exception e) {

	
		
		try {
			String url = getQueueUrl();
			logger.warn("problem receiving message from queue: " + url, e);
		} catch (RuntimeException e2) {
			logger.warn("problem receiving message from queue: " + queueName, e2);

		}

		

		try {
			
			logger.info("sleeping for {}ms due to {} failures", getBackoffInterval(), failureCount.get());
			Thread.sleep(getBackoffInterval());
		} catch (InterruptedException x) {
		}
		logger.info("continuing");

	}

	public Observable<SQSMessage> getObservable() {
		return publishSubject;
	}
	
	protected long getBackoffInterval() {

		return Math.min(Math.round(Math.pow(2, failureCount.getAndIncrement())) * backoffMutiplierMillis, TimeUnit.SECONDS.toMillis(60));
		
	
	}
}
