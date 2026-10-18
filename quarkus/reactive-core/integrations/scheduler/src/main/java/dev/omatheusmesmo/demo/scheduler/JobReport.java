package dev.omatheusmesmo.demo.scheduler;

import dev.omatheusmesmo.demo.common.ThreadReport;

public record JobReport(int executions, ThreadReport lastThread) {
}
