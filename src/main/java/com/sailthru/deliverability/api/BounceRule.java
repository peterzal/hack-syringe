package com.sailthru.deliverability.api;

import java.util.regex.Pattern;

public class BounceRule {

    private Pattern pattern;
    private BounceRuleType type;
    private BounceRuleClassification classification;

    public BounceRule(String pattern, String type, String classification) {
        this.pattern = Pattern.compile(pattern);
        this.type = BounceRuleType.valueOf(type);
        this.classification = BounceRuleClassification.valueOf(classification);
    }

    public boolean match(String keyToMatch) {
        return pattern.matcher(keyToMatch).find();
    }

    private Pattern getPattern() {
        return this.pattern;
    }

    private BounceRuleType getType() {
        return this.type;
    }

    public BounceRuleClassification getClassification() {
        return this.classification;
    }

    public String toString() {
        return "BounceRule(pattern=" + this.getPattern() + ", type=" + this.getType() + ", classification=" + this.getClassification() + ")";
    }
}
