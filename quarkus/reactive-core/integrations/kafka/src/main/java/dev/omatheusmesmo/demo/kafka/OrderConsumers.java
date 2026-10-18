package dev.omatheusmesmo.demo.kafka;

import dev.omatheusmesmo.demo.common.ThreadReport;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;

/**
 * Three consumers doing the same work in the three models. The thread is captured when the consumer is called,
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

    private static void sleep(Order order) {
        try {
            Thread.sleep(order.processing());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
