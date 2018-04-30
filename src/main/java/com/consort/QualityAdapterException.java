package com.consort;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Getter
@JsonIgnoreProperties({"cause", "stackTrace", "localizedMessage", "suppressed"})
public class QualityAdapterException extends Exception {

    private final int status;

    private final String message;

    private final String location;

    private final String time;

    private final StackTraceElement[] trace;

    public QualityAdapterException(Errors err, String location) {
        super(err.getMessage());
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("UTC").normalized());
        this.status = err.getStatus();
        this.message = err.getMessage();
        this.location = location;
        this.trace = this.getStackTrace();
        this.time = zonedDateTime.toString();
    }
}
