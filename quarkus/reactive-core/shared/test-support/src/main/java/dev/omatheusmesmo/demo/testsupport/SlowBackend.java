package dev.omatheusmesmo.demo.testsupport;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;

/**
 * A WireMock server on a random port that answers every {@code GET} with a fixed body after a fixed delay.
 *
 * <p>Arguments: {@value #PROPERTY} (required, the configuration key that receives the base URL),
 * {@value #DELAY_MS} (default {@code 0}) and {@value #BODY} (default {@code {}}). Tests can change the stub at
 * runtime through a {@link WireMockServer} field annotated with {@link InjectSlowBackend}. The server runs with
 * enough threads that concurrent calls from a test are never queued by WireMock itself.
 */
public class SlowBackend implements QuarkusTestResourceLifecycleManager {

    public static final String PROPERTY = "property";
    public static final String DELAY_MS = "delay-ms";
    public static final String BODY = "body";

    private static final int CONTAINER_THREADS = 100;

    private String property;
    private int delayMs;
    private String body;
    private WireMockServer server;

    @Override
    public void init(Map<String, String> args) {
        property = args.get(PROPERTY);
        if (property == null || property.isBlank()) {
            throw new IllegalArgumentException("SlowBackend requires the '" + PROPERTY + "' argument");
        }
        delayMs = Integer.parseInt(args.getOrDefault(DELAY_MS, "0"));
        body = args.getOrDefault(BODY, "{}");
    }

    @Override
    public Map<String, String> start() {
        server = new WireMockServer(options().dynamicPort().containerThreads(CONTAINER_THREADS));
        server.start();
        server.stubFor(get(anyUrl()).willReturn(okJson(body).withFixedDelay(delayMs)));
        return Map.of(property, server.baseUrl());
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(server,
                new TestInjector.AnnotatedAndMatchesType(InjectSlowBackend.class, WireMockServer.class));
    }

    public int receivedRequests() {
        return server.countRequestsMatching(anyRequestedFor(anyUrl()).build()).getCount();
    }
}
