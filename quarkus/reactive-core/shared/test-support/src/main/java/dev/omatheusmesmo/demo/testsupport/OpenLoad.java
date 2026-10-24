package dev.omatheusmesmo.demo.testsupport;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;

/**
 * Sends GET requests at a fixed arrival rate, whatever the server answers: the open model of the benchmark, scaled
 * down to a test.
 *
 * <p>Each request starts on schedule even when earlier ones are still waiting, and gives up after the timeout by
 * closing its HTTP/1.1 connection, like a client with a deadline of its own. The JDK client runs on platform
 * threads, for the same reason as {@link Concurrently}.
 */
public final class OpenLoad {

    /**
     * The status recorded for a request that got no response before the timeout.
     */
    public static final int NO_RESPONSE = 0;

    private OpenLoad() {
    }

    public static List<Integer> get(URI uri, int perSecond, int requests, Duration timeout) {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).build();
        long interval = Duration.ofSeconds(1).toNanos() / perSecond;
        try (HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
            List<CompletableFuture<Integer>> responses = new ArrayList<>(requests);
            long start = System.nanoTime();
            for (int i = 0; i < requests; i++) {
                LockSupport.parkNanos(start + i * interval - System.nanoTime());
                responses.add(client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                        .thenApply(HttpResponse::statusCode)
                        .exceptionally(failure -> NO_RESPONSE));
            }
            return responses.stream().map(CompletableFuture::join).toList();
        }
    }
}
