package com.sailthru.deliverability;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SQSProducerConfig {

    @Value("${queue.endpoint}")
    private String endpoint;

    @Value("${cloud.aws.region.static}")
    String awsRegion;

    @Bean
    @Qualifier("amazonSQSWithEndpoint")
    public AmazonSQS createAmazonSQS() {
        return AmazonSQSClientBuilder.standard()
                                     .withCredentials(new DefaultAWSCredentialsProviderChain())
                                     .withEndpointConfiguration(endpointConfiguration(endpoint, awsRegion))
                                     .build();
    }

    private AwsClientBuilder.EndpointConfiguration endpointConfiguration(String endpoint, String awsRegion) {
        return new AwsClientBuilder.EndpointConfiguration(endpoint, awsRegion);
    }
}