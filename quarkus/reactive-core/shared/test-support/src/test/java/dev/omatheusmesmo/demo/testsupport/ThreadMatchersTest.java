package dev.omatheusmesmo.demo.testsupport;

import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnDuplicatedContext;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnEventLoop;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnVirtualThread;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnWorker;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ThreadMatchersTest {

    private static final Map<String, Object> EVENT_LOOP = report("vert.x-eventloop-thread-1", false, true, true);
    private static final Map<String, Object> WORKER = report("executor-thread-1", false, false, true);
    private static final Map<String, Object> VIRTUAL = report("quarkus-virtual-thread-0", true, false, true);
    private static final Map<String, Object> ROOT_CONTEXT_WORKER = report("executor-thread-2", false, false, false);

    @Test
    void recognisesAnEventLoop() {
        assertThat(EVENT_LOOP, ranOnEventLoop());
        assertThat(WORKER, not(ranOnEventLoop()));
        assertThat(VIRTUAL, not(ranOnEventLoop()));
    }

    @Test
    void recognisesAWorker() {
        assertThat(WORKER, ranOnWorker());
        assertThat(EVENT_LOOP, not(ranOnWorker()));
        assertThat(VIRTUAL, not(ranOnWorker()));
    }

    @Test
    void recognisesAVirtualThread() {
        assertThat(VIRTUAL, ranOnVirtualThread());
        assertThat(WORKER, not(ranOnVirtualThread()));
        assertThat(EVENT_LOOP, not(ranOnVirtualThread()));
    }

    @Test
    void recognisesADuplicatedContext() {
        assertThat(WORKER, ranOnDuplicatedContext());
        assertThat(ROOT_CONTEXT_WORKER, not(ranOnDuplicatedContext()));
    }

    @Test
    void doesNotMatchSomethingThatIsNotAReport() {
        assertThat("executor-thread-1", not(ranOnWorker()));
    }

    private static Map<String, Object> report(String name, boolean virtual, boolean eventLoop, boolean duplicated) {
        return Map.of("name", name, "virtual", virtual, "eventLoop", eventLoop, "duplicatedContext", duplicated);
    }
}
