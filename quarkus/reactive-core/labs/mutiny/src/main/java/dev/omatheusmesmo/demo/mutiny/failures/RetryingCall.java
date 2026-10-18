package dev.omatheusmesmo.demo.mutiny.failures;

import io.smallrye.mutiny.Uni;

/**
 * Retries a failed call. A retry is a new subscription to the upstream, so the upstream must be lazy and
 * the operation safe to repeat.
 */
public final class RetryingCall {

    private final int retries;

    public RetryingCall(int retries) {
        this.retries = retries;
    }

    public <T> Uni<T> call(Uni<T> upstream) {
        return upstream.onFailure().retry().atMost(retries);
    }
}
