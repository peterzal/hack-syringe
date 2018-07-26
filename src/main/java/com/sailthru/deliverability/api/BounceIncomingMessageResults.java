package com.sailthru.deliverability.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Data representation for nested JSON object under "results" field coming from Kafka topic
 */
public final class BounceIncomingMessageResults {

    private String description;
    private String status;
    private Long code;
    private String time;
    private String from;
    private String state;

    @JsonCreator
    public BounceIncomingMessageResults(@JsonProperty("description") String description,
                                        @JsonProperty("status") String status,
                                        @JsonProperty("code") Long code,
                                        @JsonProperty("time") String time,
                                        @JsonProperty("from") String from,
                                        @JsonProperty("state") String state) {
        this.description = description;
        this.status = status;
        this.code = code;
        this.time = time;
        this.from = from;
        this.state = state;
    }

    @JsonIgnore
    public String getCodeDescrptionLog() {
        return code + "  " + description;
    }

    @JsonIgnore
    public String getStatusCodeDescriptionLog() {
        return status + " - " + code + " " + description;
    }

    public String getDescription() {
        return this.description;
    }

    public String getStatus() {
        return this.status;
    }

    public Long getCode() {
        return this.code;
    }

    public String getTime() {
        return this.time;
    }

    public String getFrom() {
        return this.from;
    }

    public String getState() {
        return this.state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BounceIncomingMessageResults that = (BounceIncomingMessageResults) o;
        return Objects.equals(description, that.description) &&
            Objects.equals(status, that.status) &&
            Objects.equals(code, that.code) &&
            Objects.equals(time, that.time) &&
            Objects.equals(from, that.from) &&
            Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, status, code, time, from, state);
    }

    @Override
    public String toString() {
        return "BounceIncomingMessageResults{" +
            "description='" + description + '\'' +
            ", status='" + status + '\'' +
            ", code=" + code +
            ", time='" + time + '\'' +
            ", from='" + from + '\'' +
            ", state='" + state + '\'' +
            '}';
    }
}
