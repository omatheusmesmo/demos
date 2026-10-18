package dev.omatheusmesmo.demo.restclient;

import dev.omatheusmesmo.demo.testsupport.SlowBackend;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.WithTestResource;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An upstream that answers every quote after 300 ms, well inside the one second timeout.
 */
@WithTestResource(value = SlowBackend.class, initArgs = {
        @ResourceArg(name = SlowBackend.PROPERTY, value = "quarkus.rest-client.upstream.url"),
        @ResourceArg(name = SlowBackend.DELAY_MS, value = "300"),
        @ResourceArg(name = SlowBackend.BODY, value = "{\"text\":\"stay reactive\"}")
})
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SlowUpstream {
}
