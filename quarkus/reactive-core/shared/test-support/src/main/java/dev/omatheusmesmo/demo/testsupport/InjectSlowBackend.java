package dev.omatheusmesmo.demo.testsupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code WireMockServer} field of a test that uses {@link SlowBackend}, so the test can change stubs.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectSlowBackend {
}
