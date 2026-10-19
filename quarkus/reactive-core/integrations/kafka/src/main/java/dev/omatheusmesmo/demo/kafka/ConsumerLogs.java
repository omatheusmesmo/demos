package dev.omatheusmesmo.demo.kafka;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConsumerLogs {

    private final ConsumerLog eventLoop = new ConsumerLog();
    private final ConsumerLog blocking = new ConsumerLog();
    private final ConsumerLog virtual = new ConsumerLog();
    private final ConsumerLog plain = new ConsumerLog();
    private final ConsumerLog transactional = new ConsumerLog();

    public ConsumerLog eventLoop() {
        return eventLoop;
    }

    public ConsumerLog blocking() {
        return blocking;
    }

    public ConsumerLog virtual() {
        return virtual;
    }

    public ConsumerLog plain() {
        return plain;
    }

    public ConsumerLog transactional() {
        return transactional;
    }

    void clear() {
        eventLoop.clear();
        blocking.clear();
        virtual.clear();
        plain.clear();
        transactional.clear();
    }
}
