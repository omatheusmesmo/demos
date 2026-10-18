package dev.omatheusmesmo.demo.kafka;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConsumerLogs {

    private final ConsumerLog eventLoop = new ConsumerLog();
    private final ConsumerLog blocking = new ConsumerLog();
    private final ConsumerLog virtual = new ConsumerLog();

    public ConsumerLog eventLoop() {
        return eventLoop;
    }

    public ConsumerLog blocking() {
        return blocking;
    }

    public ConsumerLog virtual() {
        return virtual;
    }

    void clear() {
        eventLoop.clear();
        blocking.clear();
        virtual.clear();
    }
}
