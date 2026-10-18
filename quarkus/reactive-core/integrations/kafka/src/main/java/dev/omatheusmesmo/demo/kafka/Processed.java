package dev.omatheusmesmo.demo.kafka;

import dev.omatheusmesmo.demo.common.ThreadReport;

/**
 * An order a consumer finished, with the thread it was handed to.
 */
public record Processed(int sequence, ThreadReport thread) {
}
