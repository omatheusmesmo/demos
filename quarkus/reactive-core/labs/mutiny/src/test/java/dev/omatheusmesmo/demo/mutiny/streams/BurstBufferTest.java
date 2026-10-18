package dev.omatheusmesmo.demo.mutiny.streams;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class BurstBufferTest {

    private static final int BURST = 100;

    @Test
    void aBoundedBufferFailsWhenTheConsumerCannotKeepUp() {
        AssertSubscriber<Integer> slowConsumer = new BurstBuffer(10).absorb(burst())
                .subscribe().withSubscriber(AssertSubscriber.create(1));

        slowConsumer.awaitFailure(Duration.ofSeconds(5)).assertFailedWith(BackPressureFailure.class);
    }

    @Test
    void aBufferLargeEnoughAbsorbsTheBurst() {
        AssertSubscriber<Integer> consumer = new BurstBuffer(BURST).absorb(burst())
                .subscribe().withSubscriber(AssertSubscriber.create(1));

        consumer.request(BURST - 1);

        consumer.awaitCompletion(Duration.ofSeconds(5)).assertItems(range());
    }

    private static Multi<Integer> burst() {
        return Multi.createFrom().emitter(emitter -> {
            for (int i = 0; i < BURST; i++) {
                emitter.emit(i);
            }
            emitter.complete();
        }, BackPressureStrategy.IGNORE);
    }

    private static Integer[] range() {
        Integer[] items = new Integer[BURST];
        for (int i = 0; i < BURST; i++) {
            items[i] = i;
        }
        return items;
    }
}
