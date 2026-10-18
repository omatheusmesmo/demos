package dev.omatheusmesmo.demo.testsupport;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

/**
 * Fires HTTP requests in parallel, all released at the same instant.
 *
 * <p>Requests run on platform threads on purpose: a virtual thread in the test client could report pinning and
 * pollute {@code @ShouldNotPin} assertions about the code under test.
 */
public final class Concurrently {

    private Concurrently() {
    }

    public static List<Response> get(int requests, String path) {
        CountDownLatch go = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(requests)) {
            List<Future<Response>> pending = IntStream.range(0, requests)
                    .mapToObj(i -> executor.submit(() -> {
                        go.await();
                        return RestAssured.get(path);
                    }))
                    .toList();
            go.countDown();
            return pending.stream().map(Concurrently::join).toList();
        }
    }

    private static Response join(Future<Response> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a response", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("A concurrent request failed", e.getCause());
        }
    }
}
