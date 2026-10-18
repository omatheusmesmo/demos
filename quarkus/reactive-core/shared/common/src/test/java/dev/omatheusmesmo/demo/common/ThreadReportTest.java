package dev.omatheusmesmo.demo.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ThreadReportTest {

    @Test
    void capturesAPlatformThreadOutsideVertx() {
        ThreadReport report = ThreadReport.capture();

        assertEquals(Thread.currentThread().getName(), report.name());
        assertFalse(report.virtual());
        assertFalse(report.eventLoop());
        assertFalse(report.duplicatedContext());
    }

    @Test
    void capturesAVirtualThread() throws InterruptedException {
        AtomicReference<ThreadReport> captured = new AtomicReference<>();

        Thread.ofVirtual().name("probe-vt").start(() -> captured.set(ThreadReport.capture())).join();

        assertEquals("probe-vt", captured.get().name());
        assertTrue(captured.get().virtual());
        assertFalse(captured.get().eventLoop());
    }
}
