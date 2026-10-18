package dev.omatheusmesmo.demo.threading.errors;

import dev.omatheusmesmo.demo.common.ErrorResponse;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class ErrorMappers {

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> map(RuntimeException exception) {
        int fallback = exception instanceof WebApplicationException web ? web.getResponse().getStatus() : 500;
        ErrorResponse body = ErrorResponse.from(exception, fallback);
        return RestResponse.status(RestResponse.Status.fromStatusCode(body.status()), body);
    }
}
