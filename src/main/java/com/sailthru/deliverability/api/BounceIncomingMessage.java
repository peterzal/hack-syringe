package com.sailthru.deliverability.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sailthru.deliverability.core.BounceClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Data representation of objects being read from Kafka topic
 */
public final class BounceIncomingMessage {

    private static final Logger LOGGER = LoggerFactory.getLogger(BounceIncomingMessage.class);
    private String envelope;
    private String recipient;
    private String xtmid;
    private List<BounceIncomingMessageResults> results;

    @JsonCreator
    public BounceIncomingMessage(@JsonProperty("envelope") String envelope,
                                 @JsonProperty("recipient") String recipient,
                                 @JsonProperty("x-tm-id") String xtmid,
                                 @JsonProperty("results") List<BounceIncomingMessageResults> results) {
        this.envelope = envelope;
        this.recipient = recipient;
        this.xtmid = xtmid;
        this.results = results;
    }

    public String getEnvelope() {
        return this.envelope;
    }

    public String getRecipient() {
        return this.recipient;
    }

    public String getXtmid() {
        return this.xtmid;
    }

    public List<BounceIncomingMessageResults> getResults() {
        return this.results;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BounceIncomingMessage that = (BounceIncomingMessage) o;
        return Objects.equals(envelope, that.envelope) &&
            Objects.equals(recipient, that.recipient) &&
            Objects.equals(xtmid, that.xtmid) &&
            Objects.equals(results, that.results);
    }

    @Override
    public int hashCode() {
        return Objects.hash(envelope, recipient, xtmid, results);
    }

    public String toJsonString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            // TODO: should we throw / retry in this case?
            LOGGER.error("Failed to parse SQS message: JSONProcessingException");
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public String toString() {
        return "BounceIncomingMessage{" +
            "envelope='" + envelope + '\'' +
            ", recipient='" + recipient + '\'' +
            ", x-tm-id='" + xtmid + '\'' +
            ", results=" + results +
            '}';
    }
}

