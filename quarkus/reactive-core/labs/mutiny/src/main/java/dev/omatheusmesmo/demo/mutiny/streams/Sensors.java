package dev.omatheusmesmo.demo.mutiny.streams;

import io.smallrye.mutiny.Multi;
import java.util.function.IntSupplier;

/**
 * An endless stream of sensor readings that respects demand: a reading is taken only when the subscriber asks
 * for one.
 */
public final class Sensors {

    private final IntSupplier sensor;

    public Sensors(IntSupplier sensor) {
        this.sensor = sensor;
    }

    public Multi<Integer> readings() {
        return Multi.createFrom().generator(() -> sensor, (source, emitter) -> {
            emitter.emit(source.getAsInt());
            return source;
        });
    }
}
