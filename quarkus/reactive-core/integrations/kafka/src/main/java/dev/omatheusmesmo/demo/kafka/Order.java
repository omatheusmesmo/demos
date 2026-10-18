package dev.omatheusmesmo.demo.kafka;

import java.time.Duration;

/**
 * An order as it travels on Kafka: {@code sequence:processingMillis}.
 */
public record Order(int sequence, Duration processing) {

    static Order parse(String payload) {
        String[] parts = payload.split(":");
        return new Order(Integer.parseInt(parts[0]), Duration.ofMillis(Long.parseLong(parts[1])));
    }
}
