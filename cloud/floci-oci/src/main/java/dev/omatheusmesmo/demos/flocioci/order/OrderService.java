package dev.omatheusmesmo.demos.flocioci.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Places an order across four services: KMS encrypts the card, Oracle Database stores the order, Object Storage keeps
 * the receipt and Queue carries the event. The database transaction wraps the whole method, so an OCI failure rolls
 * the order back. Calls already made to OCI are not undone: a receipt written before a failed publish stays in the
 * bucket, which a production service would handle with an outbox.
 */
@ApplicationScoped
public class OrderService {

    private final CardVault cards;
    private final ReceiptStore receipts;
    private final OrderEvents events;
    private final ObjectMapper json;

    public OrderService(CardVault cards, ReceiptStore receipts, OrderEvents events, ObjectMapper json) {
        this.cards = cards;
        this.receipts = receipts;
        this.events = events;
        this.json = json;
    }

    @Transactional
    public OrderView place(PlaceOrderRequest request) {
        Order order = new Order();
        order.customer = request.customer();
        order.total = request.total();
        order.cardLast4 = request.cardNumber().substring(12);
        order.cardCiphertext = cards.encrypt(request.cardNumber());
        order.createdAt = Instant.now();
        order.persistAndFlush();

        order.receiptObject = receipts.objectName(order.id);
        OrderView view = OrderView.of(order);
        receipts.put(order.receiptObject, write(view));
        events.publish(write(Map.of("type", "order-created", "order", view)));
        return view;
    }

    public Optional<OrderView> find(long id) {
        return Order.<Order>findByIdOptional(id).map(OrderView::of);
    }

    public Optional<String> receipt(long id) {
        return Order.<Order>findByIdOptional(id).map(order -> receipts.get(order.receiptObject));
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize " + value, e);
        }
    }
}
