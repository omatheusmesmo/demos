package dev.omatheusmesmo.demos.flocioci.support;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.List;

/**
 * floci-oci in a container. The emulator hands URLs back to its clients (a vault's crypto endpoint, a queue's
 * messages endpoint) built from {@code FLOCI_OCI_BASE_URL}, so the container is bound to a host port chosen up front
 * and told that address. With a random mapped port, those URLs would point at {@code localhost:4599}.
 */
public class FlociOciContainer extends GenericContainer<FlociOciContainer> {

    public static final String IMAGE = "floci/floci-oci:0.7.0";
    public static final String DEFAULT_TENANCY =
            "ocid1.tenancy.oc1..flocilocaltenancy0000000000000000000000000000000000000000";
    private static final int PORT = 4599;

    private final int hostPort;

    public FlociOciContainer() {
        super(DockerImageName.parse(IMAGE));
        this.hostPort = freePort();
        withExposedPorts(PORT);
        withCreateContainerCmdModifier(cmd -> cmd.getHostConfig()
                .withPortBindings(List.of(new PortBinding(Ports.Binding.bindPort(hostPort), new ExposedPort(PORT)))));
        withEnv("FLOCI_OCI_BASE_URL", "http://localhost:" + hostPort);
        waitingFor(Wait.forHttp("/_floci/health").forPort(PORT).forStatusCode(200));
    }

    public String endpoint() {
        return "http://localhost:" + hostPort;
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
