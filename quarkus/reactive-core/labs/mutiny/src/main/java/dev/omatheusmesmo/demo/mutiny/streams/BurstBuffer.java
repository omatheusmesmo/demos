package dev.omatheusmesmo.demo.mutiny.streams;

import io.smallrye.mutiny.Multi;

/**
 * Absorbs bursts from a source that cannot slow down, such as ticks or user input. A bounded buffer fails with
 * {@code BackPressureFailure} once it is full, instead of growing until the heap runs out.
 */
public final class BurstBuffer {

    private final int size;

    public BurstBuffer(int size) {
        this.size = size;
    }

    public <T> Multi<T> absorb(Multi<T> burst) {
        return burst.onOverflow().buffer(size);
    }
}
