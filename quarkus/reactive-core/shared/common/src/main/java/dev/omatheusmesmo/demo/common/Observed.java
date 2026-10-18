package dev.omatheusmesmo.demo.common;

/**
 * Response body of every comparison endpoint: what was computed and which thread computed it.
 */
public record Observed<T>(T result, ThreadReport thread) {

    public static <T> Observed<T> of(T result) {
        return new Observed<>(result, ThreadReport.capture());
    }
}
