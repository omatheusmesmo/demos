package dev.omatheusmesmo.demos.invoicing.web;

import dev.omatheusmesmo.demos.invoicing.api.InvoiceDto;
import dev.omatheusmesmo.demos.invoicing.domain.Invoice;
import dev.omatheusmesmo.demos.invoicing.service.InvoiceService;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

@ManagedBean(name = "invoiceList")
@RequestScoped
public class InvoiceListBean {

    @EJB
    private InvoiceService service;

    public List<InvoiceDto> getInvoices() {
        List<InvoiceDto> result = new ArrayList<InvoiceDto>();
        for (Invoice invoice : service.list()) {
            result.add(InvoiceDto.of(invoice));
        }
        return result;
    }

    public String getJavaVersion() {
        return System.getProperty("java.version");
    }
}
