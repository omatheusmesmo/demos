package dev.omatheusmesmo.demos.flocioci.order;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * What the API returns for an order. Also the body of the receipt and of the queue event, so the three always agree.
 */
public record OrderView(long id, String customer, BigDecimal total, String cardLast4, String receiptObject,
                        Instant createdAt) {

    static OrderView of(Order order) {
        return new OrderView(order.id, order.customer, order.total, order.cardLast4, order.receiptObject,
                order.createdAt);
    }
}
