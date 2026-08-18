package com.vitalair.exception;

/** Thrown when every upstream AQI data source in the fallback chain fails. */
public class UpstreamDataException extends RuntimeException {
    public UpstreamDataException(String message) {
        super(message);
    }

    public UpstreamDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
