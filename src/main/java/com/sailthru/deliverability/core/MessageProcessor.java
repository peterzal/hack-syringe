package com.sailthru.deliverability.core;

import com.amazonaws.services.sqs.AmazonSQS;
import com.sailthru.deliverability.api.BounceIncomingMessage;
import com.sailthru.deliverability.api.BounceOutgoingMessage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class MessageProcessor {

    @Value("${sqs.queue.name}")
    private String sqsQueueName;

    private String sqsQueueUrl;

    private BounceClassifier bounceClassifier;

    private final AmazonSQS amazonSQS;
    private final Counter successes;
    private final Counter failures;

    static final int MAX_PROCESS_RETIRES = 2;
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageProcessor.class);

    @Autowired
    public MessageProcessor(@Qualifier("amazonSQSWithEndpoint") AmazonSQS amazonSQS, BounceClassifier bounceClassifier, MeterRegistry meterRegistry) {
        this.amazonSQS = amazonSQS;
        this.bounceClassifier = bounceClassifier;
        this.successes = meterRegistry.counter("messages_total", "result", "success");
        this.failures = meterRegistry.counter("messages_total", "result", "failure");
    }

    @PostConstruct
    public void init() {
        // This must be set in a @PostConstruct to ensure AmazonSQS is set up with the query param QueueName
        this.sqsQueueUrl = amazonSQS.getQueueUrl(sqsQueueName).getQueueUrl();
    }

    @KafkaListener(topics = "${kafka.topic}")
    public void receive(BounceIncomingMessage bounceIncomingMessage) {
        LOGGER.info("Received message as BounceIncomingMessage class: {}", bounceIncomingMessage.toJsonString());

        for (int numRetries = 0; numRetries < MAX_PROCESS_RETIRES; numRetries++) {
            if (attemptMessageProcess(bounceIncomingMessage)) {
                return;
            }
        }
        // Failed after max attempts - log so we can manually retry it later
        LOGGER.error("Failed after max retries {}  to process message. This message will not be retried. BounceIncomingMessage: {}",
            MAX_PROCESS_RETIRES, bounceIncomingMessage.toJsonString());
    }

    private boolean attemptMessageProcess(BounceIncomingMessage bounceIncomingMessage) {
        try {
            BounceOutgoingMessage result = bounceClassifier.buildBounceJSON(bounceIncomingMessage);
            LOGGER.info("Sending message to SQS, queue URL: {}  and data: {}", sqsQueueUrl, result);
            amazonSQS.sendMessage(sqsQueueUrl, result.toJsonString());
            successes.increment();
            return true;
        } catch (Exception e) {
            LOGGER.error("Message processing failed for BounceIncomingMessage: {} with exception: {}", bounceIncomingMessage.toJsonString(), e);
            failures.increment();
            return false;
        }
    }
}
