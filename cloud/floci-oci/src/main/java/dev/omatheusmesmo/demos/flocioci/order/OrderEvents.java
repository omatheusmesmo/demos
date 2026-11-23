package dev.omatheusmesmo.demos.flocioci.order;

import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.PutMessagesDetails;
import com.oracle.bmc.queue.model.PutMessagesDetailsEntry;
import com.oracle.bmc.queue.requests.PutMessagesRequest;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Publishes order events to an OCI Queue, where a downstream consumer (billing, shipping) would pick them up.
 */
@ApplicationScoped
public class OrderEvents {

    private final QueueClient queue;
    private final String queueId;

    public OrderEvents(QueueClient queue, OrdersConfig config) {
        this.queue = queue;
        this.queueId = config.events().queueId();
    }

    public void publish(String json) {
        queue.putMessages(PutMessagesRequest.builder()
                .queueId(queueId)
                .putMessagesDetails(PutMessagesDetails.builder()
                        .messages(List.of(PutMessagesDetailsEntry.builder().content(json).build()))
                        .build())
                .build());
    }
}
