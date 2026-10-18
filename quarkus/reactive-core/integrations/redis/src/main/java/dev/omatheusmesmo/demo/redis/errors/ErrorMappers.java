package dev.omatheusmesmo.demo.redis.errors;

import dev.omatheusmesmo.demo.common.ErrorResponse;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Maps every failure to an {@link ErrorResponse}. The parameter is {@link Exception}, not {@link RuntimeException}:
 * the reactive REST Client fails with a checked Vert.x {@code TimeoutException}, which a narrower mapper never sees.
 */
public class ErrorMappers {

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> map(Exception exception) {
        int fallback = exception instanceof WebApplicationException web ? web.getResponse().getStatus() : 500;
        ErrorResponse body = ErrorResponse.from(exception, fallback);
        return RestResponse.status(RestResponse.Status.fromStatusCode(body.status()), body);
    }
}
