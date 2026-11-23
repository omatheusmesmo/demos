package dev.omatheusmesmo.demos.flocioci.expense;

import com.oracle.bmc.queue.model.GetMessage;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Consumes the receipt-processing queue. A job is deleted only after it was handled; a job that fails is left alone,
 * so the queue delivers it again after the visibility timeout and, after too many deliveries, moves it to the
 * dead-letter queue for someone to look at.
 */
@ApplicationScoped
public class ReceiptWorker {

    private static final Logger LOG = Logger.getLogger(ReceiptWorker.class);
    private static final int BATCH = 10;

    private final ReceiptJobs jobs;
    private final ReceiptProcessor processor;

    public ReceiptWorker(ReceiptJobs jobs, ReceiptProcessor processor) {
        this.jobs = jobs;
        this.processor = processor;
    }

    @Scheduled(every = "${expenses.jobs.poll-every}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void poll() {
        for (GetMessage job : jobs.take(BATCH)) {
            try {
                processor.process(UUID.fromString(job.getContent().trim()));
                jobs.done(job);
            } catch (RuntimeException e) {
                LOG.warnf("Receipt job '%s' failed on delivery %d, leaving it to the queue: %s",
                        job.getContent(), job.getDeliveryCount(), e.getMessage());
            }
        }
    }
}
