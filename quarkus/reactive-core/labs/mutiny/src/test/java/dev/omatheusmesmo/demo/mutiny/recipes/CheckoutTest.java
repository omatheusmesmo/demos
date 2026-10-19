package dev.omatheusmesmo.demo.mutiny.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

class CheckoutTest {

    private final List<String> auditLog = new CopyOnWriteArrayList<>();
    private final Checkout checkout = new Checkout(order -> Uni.createFrom().voidItem()
            .invoke(() -> auditLog.add(order)));

    @Test
    void placingAnOrderWritesItToTheAuditLog() {
        String confirmation = checkout.place("order-42").await().atMost(Duration.ofSeconds(5));

        assertEquals("placed:order-42", confirmation);
        assertEquals(List.of("order-42"), auditLog);
    }
}
