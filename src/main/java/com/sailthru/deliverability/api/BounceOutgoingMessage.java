package com.sailthru.deliverability.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sailthru.deliverability.core.BounceClassifier;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * Result data object to be sent to SQS
 */
public class BounceOutgoingMessage {

    private static final Logger LOGGER = LoggerFactory.getLogger(BounceOutgoingMessage.class);

    private String timestamp;
    private @JsonProperty("to_list") String toList;
    private String msgid;
    private String log;

    private String status;

    @JsonCreator
    BounceOutgoingMessage(@JsonProperty("timestamp") String timestamp,
                          @JsonProperty("to_list") String toList,
                          @JsonProperty("msgid") String msgid,
                          @JsonProperty("log") String log,
                          @JsonProperty("status") String status) {

        this.timestamp = timestamp;
        this.toList = toList;
        this.msgid = msgid;
        this.log = log;
        this.status = status;
    }

    public static ResultMessageBuilder builder() {
        return new ResultMessageBuilder();
    }

    public String toJsonString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to parse SQS message: JSONProcessingException", e);
        }
        return "";
    }

    @Override
    public String toString() {
        return toJsonString();
    }

    @JsonProperty("timestamp")
    public String getTimestamp() {
        return this.timestamp;
    }

    @JsonProperty("to_list")
    public String getToList() {
        return this.toList;
    }

    @JsonProperty("msgid")
    public String getMsgid() {
        return this.msgid;
    }

    @JsonProperty("log")
    public String getLog() {
        return this.log;
    }

    @JsonProperty("status")
    public String getStatus() {
        return this.status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BounceOutgoingMessage that = (BounceOutgoingMessage) o;
        return Objects.equals(timestamp, that.timestamp) &&
            Objects.equals(toList, that.toList) &&
            Objects.equals(msgid, that.msgid) &&
            Objects.equals(log, that.log) &&
            Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toList, msgid, log, status);
    }

    public static class ResultMessageBuilder {
        private String timestamp;
        private String toList;
        private String msgid;
        private String log;
        private String status;

        ResultMessageBuilder() {
        }

        public BounceOutgoingMessage.ResultMessageBuilder timestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public BounceOutgoingMessage.ResultMessageBuilder toList(String toList) {
            this.toList = toList;
            return this;
        }

        public BounceOutgoingMessage.ResultMessageBuilder msgid(String msgid) {
            this.msgid = msgid;
            return this;
        }

        public BounceOutgoingMessage.ResultMessageBuilder log(String log) {
            this.log = log;
            return this;
        }

        public BounceOutgoingMessage.ResultMessageBuilder status(String status) {
            this.status = status.toLowerCase();
            return this;
        }

        public BounceOutgoingMessage build() {
            return new BounceOutgoingMessage(timestamp, toList, msgid, log, status);
        }

        public String toString() {
            return "BounceOutgoingMessage.ResultMessageBuilder(timestamp=" + this.timestamp +
                ",toList=" + this.toList +
                ", msgid=" + this.msgid +
                ", log=" + this.log +
                ", status=" + this.status + ")";
        }
    }
}
