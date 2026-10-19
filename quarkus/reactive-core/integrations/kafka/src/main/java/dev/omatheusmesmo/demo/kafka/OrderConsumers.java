package dev.omatheusmesmo.demo.kafka;

import dev.omatheusmesmo.demo.common.ThreadReport;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Incoming;

/**
 * Three consumers doing the same work in the three models, plus two that only record their thread: one with no
 * annotation and one with {@code @Transactional}, to show how Reactive Messaging picks the thread. The thread is captured when the consumer is called,
 * because the event loop consumer finishes its non-blocking delay on another thread.
 */
@ApplicationScoped
public class OrderConsumers {

    private final ConsumerLogs logs;

    public OrderConsumers(ConsumerLogs logs) {
        this.logs = logs;
    }

    @Incoming("event-loop")
    public Uni<Void> onEventLoop(String payload) {
        Order order = Order.parse(payload);
        ThreadReport thread = ThreadReport.capture();
        ConsumerLog log = logs.eventLoop();
        return log.inFlight()
                .track(Uni.createFrom().voidItem().onItem().delayIt().by(order.processing()))
                .invoke(() -> log.finished(new Processed(order.sequence(), thread)));
    }

    @Incoming("blocking")
    @Blocking
    public void onWorker(String payload) {
        process(payload, logs.blocking());
    }

    @Incoming("plain")
    public void onPlainSignature(String payload) {
        record(payload, logs.plain());
    }

    @Incoming("transactional")
    @Transactional
    public void onTransactional(String payload) {
        record(payload, logs.transactional());
    }

    @Incoming("virtual")
    @RunOnVirtualThread
    public void onVirtualThread(String payload) {
        process(payload, logs.virtual());
    }

    private static void process(String payload, ConsumerLog log) {
        Order order = Order.parse(payload);
        ThreadReport thread = ThreadReport.capture();
        log.inFlight().track(() -> {
            sleep(order);
            return order;
        });
        log.finished(new Processed(order.sequence(), thread));
    }

    private static void record(String payload, ConsumerLog log) {
        log.finished(new Processed(Order.parse(payload).sequence(), ThreadReport.capture()));
    }

    private static void sleep(Order order) {
        try {
            Thread.sleep(order.processing());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
