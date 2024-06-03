package com.remiges.logharbour.exception;

public class LogQueryException extends RuntimeException {
    public LogQueryException(String message) {
        super(message);
    }

    public LogQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
