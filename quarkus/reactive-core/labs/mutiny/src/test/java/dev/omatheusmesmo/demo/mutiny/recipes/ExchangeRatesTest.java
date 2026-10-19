package dev.omatheusmesmo.demo.mutiny.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ExchangeRatesTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final AtomicInteger remoteCalls = new AtomicInteger();
    private final ExchangeRates rates = new ExchangeRates(() -> "5.4" + remoteCalls.incrementAndGet());

    @Test
    void buildingAUniCallsNothing() {
        Uni<String> rate = rates.rate();

        assertEquals(0, remoteCalls.get());
    }

    @Test
    void eachSubscriptionCallsTheRemoteServiceAgain() {
        Uni<String> rate = rates.rate();

        assertEquals("5.41", rate.await().atMost(TIMEOUT));
        assertEquals("5.42", rate.await().atMost(TIMEOUT));
        assertEquals(2, remoteCalls.get());
    }

    @Test
    void passingAValueInsteadOfASupplierCallsTheRemoteServiceRightAway() {
        Uni<String> rate = rates.rateFetchedRightAway();

        assertEquals(1, remoteCalls.get());
    }

    @Test
    void memoizeSharesTheFirstResultWithEverySubscriber() {
        Uni<String> rate = rates.sharedRate();

        assertEquals("5.41", rate.await().atMost(TIMEOUT));
        assertEquals("5.41", rate.await().atMost(TIMEOUT));
        assertEquals(1, remoteCalls.get());
    }
}
