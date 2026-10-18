package dev.omatheusmesmo.demo.mutiny.streams;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.smallrye.mutiny.helpers.test.AssertMulti;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SensorsTest {

    @Test
    void aDemandDrivenSourceProducesOnlyWhatWasRequested() {
        AtomicInteger produced = new AtomicInteger();
        Sensors sensors = new Sensors(produced::incrementAndGet);

        AssertMulti.create(sensors.readings())
                .withInitialRequest(0)
                .thenRequest(3)
                .expectNext(1, 2, 3)
                .thenCancel()
                .verify();

        assertEquals(3, produced.get());
    }
}
