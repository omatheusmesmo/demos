package dev.omatheusmesmo.demo.scheduler;

import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnEventLoop;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnVirtualThread;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnWorker;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class JobsTest {

    @Test
    void aPlainScheduledMethodRunsOnAWorker() {
        Jobs.awaitExecutions("worker", 1).body("lastThread", ranOnWorker());
    }

    @Test
    void aScheduledMethodReturningAUniRunsOnTheEventLoop() {
        Jobs.awaitExecutions("event-loop", 1).body("lastThread", ranOnEventLoop());
    }

    @Test
    void aScheduledMethodWithRunOnVirtualThreadRunsOnAVirtualThread() {
        Jobs.awaitExecutions("virtual", 1).body("lastThread", ranOnVirtualThread());
    }
}
