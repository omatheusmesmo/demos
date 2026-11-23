package dev.omatheusmesmo.demos.flocioci.expense;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/expenses")
public class ExpenseResource {

    private final ExpenseService expenses;

    public ExpenseResource(ExpenseService expenses) {
        this.expenses = expenses;
    }

    @POST
    public RestResponse<OpenedExpense> open(@Valid NewExpense request) {
        OpenedExpense opened = expenses.open(request);
        return RestResponse.ResponseBuilder.<OpenedExpense>created(URI.create("expenses/" + opened.expense().id()))
                .entity(opened)
                .build();
    }

    @POST
    @Path("/{id}/submit")
    @ResponseStatus(202)
    public ExpenseView submit(UUID id) {
        return expenses.submit(id);
    }

    @GET
    @Path("/{id}")
    public ExpenseView find(UUID id) {
        return expenses.find(id);
    }

    @GET
    public List<ExpenseView> ofEmployee(@RestQuery @NotBlank String employee) {
        return expenses.ofEmployee(employee);
    }

    @GET
    @Path("/{id}/receipt")
    public RestResponse<byte[]> receipt(UUID id) {
        ExpenseService.Receipt receipt = expenses.receipt(id);
        return RestResponse.ResponseBuilder.ok(receipt.content(), receipt.contentType()).build();
    }
}
