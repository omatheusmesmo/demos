package dev.omatheusmesmo.demo.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class ErrorResponseTest {

    @Test
    void anUnexpectedFailureKeepsTheDefaultStatus() {
        ErrorResponse response = ErrorResponse.from(new IllegalStateException("boom"), 500);

        assertEquals(new ErrorResponse(500, "IllegalStateException", "boom"), response);
    }

    @Test
    void aTimeoutBecomesAGatewayTimeout() {
        ErrorResponse response = ErrorResponse.from(new TimeoutException("too slow"), 500);

        assertEquals(504, response.status());
        assertEquals("TimeoutException", response.error());
    }

    @Test
    void aTimeoutWrappedByAClientIsStillAGatewayTimeout() {
        RuntimeException wrapped = new RuntimeException("client failed", new SocketTimeoutLike("read timed out"));

        ErrorResponse response = ErrorResponse.from(wrapped, 500);

        assertEquals(504, response.status());
        assertEquals("SocketTimeoutLike", response.error());
        assertEquals("read timed out", response.message());
    }

    private static final class SocketTimeoutLike extends RuntimeException {
        SocketTimeoutLike(String message) {
            super(message);
        }
    }
}
