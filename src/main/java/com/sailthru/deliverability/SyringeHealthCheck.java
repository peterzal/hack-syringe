package com.sailthru.deliverability;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class SyringeHealthCheck extends AbstractHealthIndicator {

    private final AmazonSQS amazonSQS;
    private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    private static final String LAST_HEARTBEAT_SECONDS_AGO_METRIC_NAME = "last-heartbeat-seconds-ago";

    @Value("#{new Long('${kafka.heartbeat-threshold-ms:3000}')}")
    private Long heartBeatThresholdMs;

    @Value("${sqs.queue.name}")
    private String sqsQueueName;

    @Autowired
    public SyringeHealthCheck(KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry,
                              @Qualifier("amazonSQSWithEndpoint") AmazonSQS amazonSQS) {
        this.kafkaListenerEndpointRegistry = kafkaListenerEndpointRegistry;
        this.amazonSQS = amazonSQS;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        if (isKafkaRunning() && isAmazonSQSRunning()) {
            builder.up();
        } else {
            builder.down();
        }
    }

    private boolean isAmazonSQSRunning() {
        try {
            amazonSQS.getQueueUrl(sqsQueueName);
            return true;
        } catch (QueueDoesNotExistException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isKafkaRunning() {
        Set<String> containerIds = kafkaListenerEndpointRegistry.getListenerContainerIds();
        for(String id : containerIds) {
            if (!isSingleKafkaContainerRunning(id)) {
                return false;
            }
        }
        return true;
     }

     private boolean isSingleKafkaContainerRunning(String containerId) {
         // Because listener containers created for @KafkaListener annotations are not beans in the application context, we need
         // an "id" to reference the individual container to track its metrics
         Map<String, Map<MetricName, ? extends Metric>> kafkaMetrics =
             kafkaListenerEndpointRegistry.getListenerContainer(containerId).metrics();

         // Always expect one kafka metrics for each kafka listener container id
         if (kafkaMetrics.values().size() != 1) {
             return false;
         }
         String consumerMetricKey = kafkaMetrics.keySet().toArray()[0].toString();

         Optional<? extends Map.Entry<MetricName, ? extends Metric>> metrics = kafkaMetrics
             .get(consumerMetricKey).entrySet().stream()
             // Filter is required because the key in the metric map is a MetricKey object where we must search for the "name" field on the MetricKey
             .filter(entry -> entry.getKey().name().equals(LAST_HEARTBEAT_SECONDS_AGO_METRIC_NAME))
             .findFirst();

         if (!metrics.isPresent()) {
             return false;
         }
         try {
             Double metricValue = (Double) metrics.get().getValue().metricValue();
             return metricValue < TimeUnit.MILLISECONDS.toSeconds(heartBeatThresholdMs);
         } catch (ClassCastException e) {
             return false;
         }
     }
}
