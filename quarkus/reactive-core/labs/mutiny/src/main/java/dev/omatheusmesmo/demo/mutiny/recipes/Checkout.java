package dev.omatheusmesmo.demo.mutiny.recipes;

import io.smallrye.mutiny.Uni;
import java.util.function.Function;

/**
 * Places an order after writing it to an asynchronous audit log. The audit returns a {@link Uni}, so it goes
 * through {@code call}, which subscribes to it and waits. With {@code invoke}, the same method reference compiles,
 * but the returned {@code Uni} is dropped and the audit never runs.
 */
public final class Checkout {

    private final Function<String, Uni<Void>> audit;

    public Checkout(Function<String, Uni<Void>> audit) {
        this.audit = audit;
    }

    public Uni<String> place(String order) {
        return Uni.createFrom().item(order)
                .call(audit::apply)
                .map(placed -> "placed:" + placed);
    }
}
