package dev.omatheusmesmo.demo.scheduler;

import dev.omatheusmesmo.demo.common.ThreadReport;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * One job per model. The return type picks the thread, like a REST signature: {@code void} runs on a worker,
 * {@code Uni<Void>} on the event loop, and {@code @RunOnVirtualThread} on a virtual thread.
 *
 * <p>The logs are read through methods, never fields: callers hold a client proxy, and a field read on the proxy
 * would see the proxy's own, never-updated field instead of the bean's.
 */
@ApplicationScoped
public class ScheduledJobs {

    private final JobLog worker = new JobLog();
    private final JobLog eventLoop = new JobLog();
    private final JobLog virtual = new JobLog();

    JobReport worker() {
        return worker.report();
    }

    JobReport eventLoop() {
        return eventLoop.report();
    }

    JobReport virtual() {
        return virtual.report();
    }

    @Scheduled(every = "1s", identity = "worker")
    void onWorker() {
        worker.ran(ThreadReport.capture());
    }

    @Scheduled(every = "1s", identity = "event-loop")
    Uni<Void> onEventLoop() {
        eventLoop.ran(ThreadReport.capture());
        return Uni.createFrom().voidItem();
    }

    @Scheduled(every = "1s", identity = "virtual")
    @RunOnVirtualThread
    void onVirtualThread() {
        virtual.ran(ThreadReport.capture());
    }
}
