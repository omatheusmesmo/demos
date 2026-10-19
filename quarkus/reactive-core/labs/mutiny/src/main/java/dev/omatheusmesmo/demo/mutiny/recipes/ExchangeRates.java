package dev.omatheusmesmo.demo.mutiny.recipes;

import io.smallrye.mutiny.Uni;
import java.util.function.Supplier;

/**
 * Wraps a remote call for an exchange rate in three ways. A {@link Uni} built from a supplier is a recipe: nothing
 * runs until a subscription, and each subscription calls again. Built from a value, the call already happened
 * when the {@code Uni} was created. {@code memoize} turns the recipe into a shared result.
 */
public final class ExchangeRates {

    private final Supplier<String> fetchRate;
    private final Uni<String> shared;

    public ExchangeRates(Supplier<String> fetchRate) {
        this.fetchRate = fetchRate;
        this.shared = rate().memoize().indefinitely();
    }

    public Uni<String> rate() {
        return Uni.createFrom().item(fetchRate);
    }

    public Uni<String> rateFetchedRightAway() {
        return Uni.createFrom().item(fetchRate.get());
    }

    public Uni<String> sharedRate() {
        return shared;
    }
}
