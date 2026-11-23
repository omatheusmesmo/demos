package dev.omatheusmesmo.demos.flocioci.expense;

import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.GetMessage;
import com.oracle.bmc.queue.model.PutMessagesDetails;
import com.oracle.bmc.queue.model.PutMessagesDetailsEntry;
import com.oracle.bmc.queue.requests.DeleteMessageRequest;
import com.oracle.bmc.queue.requests.GetMessagesRequest;
import com.oracle.bmc.queue.requests.PutMessagesRequest;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

/**
 * The receipt-processing queue. A job is only the expense id: the worker reads everything else from the database,
 * so a job delivered twice or late still acts on the current state.
 */
@ApplicationScoped
public class ReceiptJobs {

    private final QueueClient queue;
    private final ExpensesConfig.Jobs jobs;

    public ReceiptJobs(QueueClient queue, ExpensesConfig config) {
        this.queue = queue;
        this.jobs = config.jobs();
    }

    public void enqueue(UUID expenseId) {
        queue.putMessages(PutMessagesRequest.builder()
                .queueId(jobs.queueId())
                .putMessagesDetails(PutMessagesDetails.builder()
                        .messages(List.of(PutMessagesDetailsEntry.builder().content(expenseId.toString()).build()))
                        .build())
                .build());
    }

    /**
     * Long-polls for jobs. Each one stays invisible to other consumers for the visibility timeout; one that is not
     * deleted in time is delivered again, and the queue moves it to its dead-letter queue after too many deliveries.
     */
    public List<GetMessage> take(int max) {
        return queue.getMessages(GetMessagesRequest.builder()
                        .queueId(jobs.queueId())
                        .visibilityInSeconds(jobs.visibilitySeconds())
                        .timeoutInSeconds(jobs.longPollSeconds())
                        .limit(max)
                        .build())
                .getGetMessages()
                .getMessages();
    }

    public void done(GetMessage job) {
        queue.deleteMessage(DeleteMessageRequest.builder()
                .queueId(jobs.queueId())
                .messageReceipt(job.getReceipt())
                .build());
    }
}
