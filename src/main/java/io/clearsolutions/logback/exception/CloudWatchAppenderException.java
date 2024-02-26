package io.clearsolutions.logback.exception;

public class CloudWatchAppenderException extends RuntimeException {
    public CloudWatchAppenderException(String message) {
        super(message);
    }
}
