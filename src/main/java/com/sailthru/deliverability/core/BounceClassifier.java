package com.sailthru.deliverability.core;

import com.sailthru.deliverability.api.BounceIncomingMessage;
import com.sailthru.deliverability.api.BounceIncomingMessageResults;
import com.sailthru.deliverability.api.BounceOutgoingMessage;
import com.sailthru.deliverability.api.BounceRule;
import com.sailthru.deliverability.api.BounceRuleClassification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BounceClassifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(BounceClassifier.class);
    private List<BounceRule> bounceRuleCollection = new ArrayList<>();
    private final String bounceRulesLocationInClassPath;

    public BounceClassifier(String bounceRulesLocationInClassPath) {
        this.bounceRulesLocationInClassPath = bounceRulesLocationInClassPath;
        if (!loadBounces()) {
            throw new IllegalStateException("Failed to load bounce rules.");
        }
        LOGGER.info("loaded bounce rules: {}", bounceRuleCollection.toString());
    }

    private boolean loadBounces() {
        List<String> bounceData;
        Resource resource = new ClassPathResource(bounceRulesLocationInClassPath);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            bounceData = br.lines().collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Error reading bounce file: {}, error: {}", bounceRulesLocationInClassPath, e);
            return false;
        }

        try {
            bounceData.forEach(this::parseBounce);
        } catch (Exception e) {
            LOGGER.error("Error during initial bounce parsing from file: {}", e);
            return false;
        }
        return true;
    }

    private void parseBounce(String line) {
        String[] configArray = line.split("\t");
        bounceRuleCollection.add(new BounceRule(configArray[0].trim(), configArray[1].trim(), configArray[2].trim()));
    }

    BounceOutgoingMessage buildBounceJSON(BounceIncomingMessage bounceIncomingMessage) {

        // The last JSON element in "results" list is the failure and the one we will send to SQS. The previous ones are retry attempts.
        List<BounceIncomingMessageResults> results = bounceIncomingMessage.getResults();

        if(results.size() == 0) {
            throw new IllegalArgumentException("Bounce incoming message \"results\" field cannot be empty");
        }
        BounceIncomingMessageResults bounceIncomingMessageResults = results.get(results.size() - 1);

        // Parse all of the bounce JSON fields
        String timestamp = bounceIncomingMessageResults.getTime();
        String toList = bounceIncomingMessage.getRecipient();
        String msgid = bounceIncomingMessage.getXtmid();

        String status = bounceIncomingMessageResults.getStatus();

        String log = bounceIncomingMessageResults.getCodeDescrptionLog();

        BounceOutgoingMessage.ResultMessageBuilder bounceJSONBuilder = BounceOutgoingMessage
            .builder()
            .log(log)
            .msgid(msgid)
            .status(status)
            .timestamp(timestamp)
            .toList(toList);

        boolean isBounceMatched = false;

        // Loop list of bounce rules that hold the categorization RegExes
        for (BounceRule bounceRule : bounceRuleCollection) {

            // Figure out what type of bounceRule we have
            String keyToMatch = bounceIncomingMessageResults.getStatusCodeDescriptionLog();

            if (bounceRule.match(keyToMatch)) {
                bounceJSONBuilder.status(bounceRule.getClassification().toString());
                isBounceMatched = true;
                break;
            }
        }

        // If no match, default to hard bounce
        if (!isBounceMatched) {
            bounceJSONBuilder.status(BounceRuleClassification.HARDBOUNCE.toString());
        }

        return bounceJSONBuilder.build();
    }

    List<BounceRule> getBounceRuleCollection() {
        return bounceRuleCollection;
    }
}
