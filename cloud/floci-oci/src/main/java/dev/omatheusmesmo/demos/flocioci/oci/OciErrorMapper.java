package dev.omatheusmesmo.demos.flocioci.oci;

import com.oracle.bmc.model.BmcException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Turns an OCI service error into a 502: the request was fine, a dependency failed. The body keeps OCI's error code
 * and {@code opc-request-id}, which is what you would quote when reporting the failure.
 */
public class OciErrorMapper {

    private static final Logger LOG = Logger.getLogger(OciErrorMapper.class);

    public record OciError(String service, int status, String code, String opcRequestId) {
    }

    @ServerExceptionMapper
    public RestResponse<OciError> map(BmcException e) {
        String service = e.getServiceDetails() == null ? "unknown" : e.getServiceDetails().getServiceName();
        LOG.errorf("OCI %s returned %d %s (opc-request-id %s): %s", service, e.getStatusCode(),
                e.getServiceCode(), e.getOpcRequestId(), e.getMessage());
        return RestResponse.status(RestResponse.Status.BAD_GATEWAY,
                new OciError(service, e.getStatusCode(), e.getServiceCode(), e.getOpcRequestId()));
    }
}
