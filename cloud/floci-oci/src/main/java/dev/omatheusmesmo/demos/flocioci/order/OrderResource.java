package dev.omatheusmesmo.demos.flocioci.order;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    private final OrderService orders;

    public OrderResource(OrderService orders) {
        this.orders = orders;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response place(@Valid PlaceOrderRequest request, @Context UriInfo uri) {
        OrderView order = orders.place(request);
        return Response.created(uri.getAbsolutePathBuilder().path(String.valueOf(order.id())).build())
                .entity(order)
                .build();
    }

    @GET
    @Path("/{id}")
    public OrderView find(long id) {
        return orders.find(id).orElseThrow(() -> new NotFoundException("Order " + id + " not found"));
    }

    @GET
    @Path("/{id}/receipt")
    public String receipt(long id) {
        return orders.receipt(id).orElseThrow(() -> new NotFoundException("Order " + id + " not found"));
    }
}
