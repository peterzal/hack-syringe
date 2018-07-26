package com.sailthru.deliverability.core;

import com.amazonaws.services.sqs.AmazonSQS;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

@Profile("MessageProcessor-test")
@Configuration
public class MessageProcessorTestConfiguration {

    @Bean
    @Qualifier("test")
    public AmazonSQS amazonSQS() {
        return mock(AmazonSQS.class);
    }
}
