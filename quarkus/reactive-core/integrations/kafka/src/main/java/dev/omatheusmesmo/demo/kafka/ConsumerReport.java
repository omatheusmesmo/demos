package dev.omatheusmesmo.demo.kafka;

import java.util.List;

public record ConsumerReport(List<Processed> processed, int peak) {
}
