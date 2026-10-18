package dev.omatheusmesmo.demo.common;

import java.util.concurrent.TimeoutException;

public record ErrorResponse(int status, String error, String message) {

    public static final int GATEWAY_TIMEOUT = 504;

    /**
     * Describes a failure. A timeout anywhere in the cause chain becomes a {@value #GATEWAY_TIMEOUT}, whichever
     * library raised it: clients wrap their own timeout types in other exceptions, so the chain is searched for
     * a {@link TimeoutException} or any exception whose class name contains {@code Timeout}.
     */
    public static ErrorResponse from(Throwable failure, int defaultStatus) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof TimeoutException || current.getClass().getSimpleName().contains("Timeout")) {
                return new ErrorResponse(GATEWAY_TIMEOUT, current.getClass().getSimpleName(), current.getMessage());
            }
        }
        return new ErrorResponse(defaultStatus, failure.getClass().getSimpleName(), failure.getMessage());
    }
}
