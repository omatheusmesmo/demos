package dev.omatheusmesmo.demos.invoicing.api;

import dev.omatheusmesmo.demos.invoicing.domain.Invoice;
import dev.omatheusmesmo.demos.invoicing.export.InvoiceXmlExporter;
import dev.omatheusmesmo.demos.invoicing.service.InvoiceService;
import dev.omatheusmesmo.demos.invoicing.service.UnknownCustomerException;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RequestScoped
@Path("/invoices")
@Produces(MediaType.APPLICATION_JSON)
public class InvoiceResource {

    @EJB
    InvoiceService service;

    @GET
    public List<InvoiceDto> list() {
        List<InvoiceDto> result = new ArrayList<InvoiceDto>();
        for (Invoice invoice : service.list()) {
            result.add(InvoiceDto.of(invoice));
        }
        return result;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(CreateInvoiceRequest request) {
        try {
            Invoice invoice = service.create(request);
            return Response.status(Response.Status.CREATED).entity(InvoiceDto.of(invoice)).build();
        } catch (UnknownCustomerException | IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorDto(e.getMessage())).build();
        }
    }

    @GET
    @Path("/{id}")
    public Response find(@PathParam("id") Long id) {
        Invoice invoice = service.find(id);
        if (invoice == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(new ErrorDto("No invoice " + id)).build();
        }
        return Response.ok(InvoiceDto.of(invoice)).build();
    }

    @GET
    @Path("/{id}/export")
    @Produces(MediaType.APPLICATION_XML)
    public Response export(@PathParam("id") Long id) {
        Invoice invoice = service.find(id);
        if (invoice == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(InvoiceXmlExporter.toXml(invoice)).build();
    }
}
