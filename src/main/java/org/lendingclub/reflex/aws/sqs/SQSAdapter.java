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

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.reactivex.Observable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.lendingclub.reflex.operator.ExceptionHandlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQSAdapter {

  static ObjectMapper mapper = new ObjectMapper();

  AtomicReference<Supplier<Boolean>> runStateRef =
      new AtomicReference<>(new AlwaysRunningSupplier());

  class AlwaysRunningSupplier implements Supplier<Boolean> {

    static final boolean RUNNING = true;

    @Override
    public Boolean get() {
      return RUNNING;
    }
  }

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

  Subject<SQSMessage> subject;
  Supplier<String> urlSupplier;
  AmazonSQSClient sqs;
  String queueName;
  boolean autoDelete = true;
  int waitTimeSeconds = 10;
  int messagesPerRequest = 10;
  AtomicBoolean running = new AtomicBoolean(false);
  String name = null;
  long backoffMutiplierMillis = 100;
  long backoffMaxMillis = TimeUnit.SECONDS.toMillis(60);
  AtomicLong successiveFailureCount = new AtomicLong(0);
  AtomicLong messageReceiveCount = new AtomicLong(0);
  AtomicLong dispatchSuccessCount = new AtomicLong(0);
  AtomicLong totalFailureCount = new AtomicLong(0);

  public SQSAdapter() {
    PublishSubject<SQSMessage> temp = PublishSubject.create();
    this.subject = temp.toSerialized();
  }

  @SuppressWarnings("unchecked")
  public <T extends SQSAdapter> T withSQSClient(AmazonSQSClient client) {

    this.sqs = client;
    return (T) this;
  }

  /**
   * SQS is implemented with long-polling. This is the number of seconds that the SQS
   * receive-message operation will block before closing and executing another. Default: 10
   *
   * @param secs
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T extends SQSAdapter> T withWaitTimeSeconds(int secs) {
    this.waitTimeSeconds = secs;
    return (T) this;
  }

  /**
   * SQS is implemented with long-polling. This is the number of seconds that the SQS
   * receive-message operation will block before closing and executing another. Default: 10
   *
   * @return number of seconds that any HTTPS call should wait
   */
  public int getWaitTimeSeconds() {
    return this.waitTimeSeconds;
  }

  /**
   * The SQS client can request between 1-10 messages per GET request. Default: 10.
   *
   * @param count
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T extends SQSAdapter> T withMaxMessagesPerRequest(int count) {
    this.messagesPerRequest = count;
    return (T) this;
  }

  public int getMessagesPerRequest() {
    return messagesPerRequest;
  }

  /**
   * If you do not specify the queue URL and the client is set to the correct region, SQSAdapter
   * will resolve the URL from the unqualified queue name. If the url is set, there is no need to
   * set the name.
   *
   * @param name
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T extends SQSAdapter> T withQueueName(String name) {
    this.queueName = name;
    return (T) this;
  }

  /**
   * Do NOT MAKE THIS PUBLIC. Unless we work out the complexities of resolving the queue name, since
   * the user may have only specified the URL and might expect the queue name to be resolved...which
   * it currently does not do.
   */
  private String getQueueName() {
    return this.queueName;
  }

  /**
   * Exponential backoff multiplier. (failureCount ^ 2 * multiplier)
   *
   * @param millis
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T extends SQSAdapter> T withBackoffMultiplierMillis(long millis) {
    assertNotStarted();
    this.backoffMutiplierMillis = millis;
    return (T) this;
  }

  public long getBackoffMultiplierMillis() {
    return backoffMutiplierMillis;
  }

  public boolean isAutoDeleteEnabled() {
    return autoDelete;
  }

  /**
   * Connects this SQSAdapter to an EventBus.
   *
   * @param bus
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T extends SQSAdapter> T withEventBus(EventBus bus) {
    return (T) connectTo(bus);
  }

  @SuppressWarnings("unchecked")
  public <T extends SQSAdapter> T withAdapterName(String name) {
    this.name = name;
    return (T) this;
  }

  public String getName() {
    return name;
  }

  @SuppressWarnings("unchecked")
  public <T extends SQSAdapter> T withAutoDeleteEnabled(boolean b) {
    assertNotStarted();
    this.autoDelete = b;
    return (T) this;
  }

  /**
   * Maximum number of milliseconds that retries will wait in the case of failure. Default: 60000ms
   * (60 seconds)
   *
   * @param millis
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T extends SQSAdapter> T withBackofMaxMillis(long millis) {
    assertNotStarted();
    this.backoffMaxMillis = millis;
    return (T) this;
  }

  /**
   * Sets the SQS queue URL that the SQSAdpater will use for polling.
   *
   * @param url
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T extends SQSAdapter> T withQueueUrl(String url) {
    assertNotStarted();
    this.urlSupplier = new SQSUrlSupplier(url);
    return (T) this;
  }

  public static SQSJsonMessageExtractor newJsonMessageExtractor() {
    return new SQSJsonMessageExtractor();
  }

  public static class SQSJsonMessageExtractor implements Function<Object, Observable<JsonNode>> {

    @Override
    public Observable<JsonNode> apply(Object t) throws Exception {

      JsonNode result = null;
      if (t instanceof SQSMessage) {
        try {
          SQSMessage m = (SQSMessage) t;
          String data = m.getMessage().getBody();
          result = mapper.readTree(data);
          if (result.path("Type").asText().equals("Notification") && result.has("Message")) {
            result = mapper.readTree(result.path("Message").asText());
          }
          return Observable.just(result);
        } catch (JsonParseException e) {
          logger.debug("could not parse", e);
          if (result == null) {
            return Observable.empty();
          } else {
            return Observable.just(result);
          }
        } catch (Exception e) {
          if (t instanceof SQSMessage) {
            logger.warn(
                "problem parsing message from queue: "
                    + SQSMessage.class.cast(t).getSQSAdapter().getQueueUrl(),
                e);
          } else {
            logger.warn("problem reading message", e);
          }
        }
      } else {
        return Observable.empty();
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

  @SuppressWarnings("unchecked")
  public synchronized <T extends SQSAdapter> T start() {
    if (running.get()) {
      logger.warn("already running");
      return (T) this;
    }

    Preconditions.checkArgument(sqs != null, "SQSClient must be set");
    if (urlSupplier == null && queueName != null) {
      urlSupplier = Suppliers.memoize(new SQSUrlSupplier(sqs, queueName));
    }

    if (urlSupplier == null) {
      throw new IllegalArgumentException("queueUrl or queueName must be set");
    }

    Runnable r =
        new Runnable() {

          public void run() {

            running.set(true);
            while (running.get()) {

              try {

                if (isRunning()) {

                  ReceiveMessageRequest rmr = new ReceiveMessageRequest();
                  rmr.setWaitTimeSeconds(getWaitTimeSeconds());
                  rmr.setMaxNumberOfMessages(getMessagesPerRequest());
                  if (urlSupplier == null) {
                    throw new IllegalArgumentException("queueUrl or queueName must be set");
                  }

                  rmr.setQueueUrl(urlSupplier.get());
                  ReceiveMessageResult result = sqs.receiveMessage(rmr);
                  List<Message> list = result.getMessages();

                  if (list != null) {
                    for (Message message : list) {
                      try {
                        messageReceiveCount.incrementAndGet();
                        if (logger.isDebugEnabled()) {
                          logger.debug("received: {}", message.getMessageId());
                        }
                        SQSMessage sqs = new SQSMessage();
                        sqs.message = message;

                        subject.onNext(sqs);
                        if (autoDelete) {
                          delete(message);
                        }
                        dispatchSuccessCount.incrementAndGet();
                        resetFailureCount();
                      } catch (Exception e) {
                        handleException(e);
                      }
                    }
                  }
                } else {
                  logger.info("{} is paused", this);
                  Thread.sleep(10000);
                }

              } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                handleException(e);
              }
            }
            logger.info("stopped");
          }
        };

    String threadNameFormat =
        String.format(
                "%s-%s",
                "SQSAdapter",
                (Strings.isNullOrEmpty(name) ? Integer.toHexString(hashCode()) : name))
            + "-%d";

    ThreadFactoryBuilder tfb =
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat(threadNameFormat);

    Thread t = tfb.build().newThread(r);
    logger.info("starting thread: {}", t);
    t.start();

    return (T) this;
  }

  public void delete(Message m) {
    String url = getQueueUrl();
    logger.info("deleting {} {}", url, m.getReceiptHandle());
    DeleteMessageRequest dmr = new DeleteMessageRequest(url, m.getReceiptHandle());
    sqs.deleteMessage(dmr);
  }

  private void resetFailureCount() {
    successiveFailureCount.set(0);
  }

  private void assertNotStarted() {
    Preconditions.checkState(running.get() == false, "start() already called");
  }

  protected void handleException(Throwable e) {

    successiveFailureCount.incrementAndGet();
    totalFailureCount.incrementAndGet();
    try {
      String url = getQueueUrl();
      logger.warn("problem receiving message from queue: " + url, e);
    } catch (RuntimeException e2) {
      logger.warn("problem receiving message from queue: " + queueName, e2);
    }

    try {

      logger.info(
          "sleeping for {}ms due to {} failures",
          getBackoffInterval(),
          successiveFailureCount.get());
      Thread.sleep(getBackoffInterval());
    } catch (InterruptedException x) {
    }
    logger.info("continuing");
  }

  public Observable<SQSMessage> getObservable() {
    return subject;
  }

  /**
   * Connects this adapter to an EventBus that will receive messages.
   *
   * @param bus
   * @return
   */
  public SQSAdapter connectTo(EventBus bus) {
    getObservable()
        .subscribe(
            ExceptionHandlers.safeConsumer(
                m -> {
                  bus.post(m);
                }));
    return this;
  }

  protected long getBackoffInterval() {

    return Math.min(
        Math.round(Math.pow(2, successiveFailureCount.getAndIncrement())) * backoffMutiplierMillis,
        TimeUnit.SECONDS.toMillis(60));
  }

  public AmazonSQSClient getSQSClient() {
    return sqs;
  }

  public AtomicLong getSuccesiveFailureCount() {
    return successiveFailureCount;
  }

  public AtomicLong getTotalFailureCount() {
    return totalFailureCount;
  }

  public AtomicLong getTotalMessagesReceivedCount() {
    return messageReceiveCount;
  }

  public AtomicLong getTotalSuccessCount() {
    return dispatchSuccessCount;
  }

  public void setRunStateSupplier(Supplier<Boolean> supplier) {
    runStateRef.set(supplier);
  }

  public boolean isRunning() {

    // There are TWO booleans in play.
    // 1) "running" determines whether the adapter has been started and not stopped
    if (running.get() == false) {
      return false;
    }

    // 2) Run state allows the adapter to be paused externally
    Supplier<Boolean> runStateSupplier = runStateRef.get();
    if (runStateSupplier == null) {
      return true;
    }
    return runStateSupplier.get();
  }
}
