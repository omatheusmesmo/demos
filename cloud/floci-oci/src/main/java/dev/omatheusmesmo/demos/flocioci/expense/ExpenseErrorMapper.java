package dev.omatheusmesmo.demos.flocioci.expense;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Maps the workflow's own exceptions to HTTP, so the service layer knows nothing about JAX-RS.
 */
public class ExpenseErrorMapper {

    public record ApiError(String message) {
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> notFound(ExpenseNotFoundException e) {
        return RestResponse.status(RestResponse.Status.NOT_FOUND, new ApiError(e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> wrongState(ExpenseStateException e) {
        return RestResponse.status(RestResponse.Status.CONFLICT, new ApiError(e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> tooLarge(ReceiptTooLargeException e) {
        return RestResponse.status(RestResponse.Status.PAYLOAD_TOO_LARGE, new ApiError(e.getMessage()));
    }
}
