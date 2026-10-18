package dev.omatheusmesmo.demo.restclient;

import dev.omatheusmesmo.demo.common.ConcurrencyTracker;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

public class Trackers {

    @Produces
    @Singleton
    ConcurrencyTracker requestsInFlight() {
        return new ConcurrencyTracker();
    }
}
