package dev.omatheusmesmo.demos.invoicing.api;

import dev.omatheusmesmo.demos.invoicing.domain.Customer;
import dev.omatheusmesmo.demos.invoicing.service.InvoiceService;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RequestScoped
@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
public class CustomerResource {

    @EJB
    InvoiceService service;

    @GET
    public List<CustomerDto> list() {
        List<CustomerDto> result = new ArrayList<CustomerDto>();
        for (Customer customer : service.customers()) {
            result.add(CustomerDto.of(customer));
        }
        return result;
    }
}
