package dev.omatheusmesmo.demo.testsupport;

import java.util.Map;
import java.util.function.Predicate;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * Hamcrest matchers for the {@code thread} object of an {@code Observed} JSON response.
 *
 * <p>Thread names follow Quarkus: {@code vert.x-eventloop-thread-N} for event loops and {@code executor-thread-N}
 * for the worker pool.
 */
public final class ThreadMatchers {

    private ThreadMatchers() {
    }

    public static Matcher<Object> ranOnEventLoop() {
        return report("ran on an event loop", r -> is(r, "eventLoop") && !is(r, "virtual")
                && name(r).startsWith("vert.x-eventloop-thread-"));
    }

    public static Matcher<Object> ranOnWorker() {
        return report("ran on a worker thread", r -> !is(r, "eventLoop") && !is(r, "virtual")
                && name(r).startsWith("executor-thread-"));
    }

    public static Matcher<Object> ranOnVirtualThread() {
        return report("ran on a virtual thread", r -> is(r, "virtual") && !is(r, "eventLoop"));
    }

    public static Matcher<Object> ranOnDuplicatedContext() {
        return report("ran on a duplicated context", r -> is(r, "duplicatedContext"));
    }

    private static Matcher<Object> report(String expectation, Predicate<Map<?, ?>> condition) {
        return new BaseMatcher<>() {
            @Override
            public boolean matches(Object actual) {
                return actual instanceof Map<?, ?> map && condition.test(map);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a thread report that " + expectation);
            }
        };
    }

    private static boolean is(Map<?, ?> report, String key) {
        return Boolean.TRUE.equals(report.get(key));
    }

    private static String name(Map<?, ?> report) {
        return String.valueOf(report.get("name"));
    }
}
