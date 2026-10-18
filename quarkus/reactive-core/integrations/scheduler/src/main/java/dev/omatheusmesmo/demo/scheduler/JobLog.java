package dev.omatheusmesmo.demo.scheduler;

import dev.omatheusmesmo.demo.common.ThreadReport;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * How often one job ran and which thread ran it last.
 */
final class JobLog {

    private final AtomicInteger executions = new AtomicInteger();
    private final AtomicReference<ThreadReport> lastThread = new AtomicReference<>();

    void ran(ThreadReport thread) {
        lastThread.set(thread);
        executions.incrementAndGet();
    }

    JobReport report() {
        return new JobReport(executions.get(), lastThread.get());
    }
}
