package org.kuxa.truenasadminworker.worker.exception;

public class TrueNasApiException extends RuntimeException {

    public TrueNasApiException(String message) {
        super(message);
    }

    public TrueNasApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
