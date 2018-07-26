package com.sailthru.deliverability;

import com.sailthru.deliverability.core.BounceClassifier;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.aws.autoconfigure.cache.ElastiCacheAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = { ElastiCacheAutoConfiguration.class })
public class SyringeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyringeApplication.class, args);
    }

    @Bean
    @Qualifier("bounceFile")
    public String bounceRulesLocationInClassPath() {
        return "bounce_rules.txt";
    }

    @Bean
    public BounceClassifier bounceLoader(@Qualifier("bounceFile") String bounceRulesLocationInClassPath) {
        return new BounceClassifier(bounceRulesLocationInClassPath);
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags() { // use tags as prefix of application
        return r -> r.config().commonTags("application", "syringe");
    }
}