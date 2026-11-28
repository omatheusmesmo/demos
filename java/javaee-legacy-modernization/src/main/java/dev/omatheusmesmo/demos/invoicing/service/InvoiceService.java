package dev.omatheusmesmo.demos.invoicing.service;

import dev.omatheusmesmo.demos.invoicing.api.CreateInvoiceRequest;
import dev.omatheusmesmo.demos.invoicing.audit.AuditLog;
import dev.omatheusmesmo.demos.invoicing.domain.Customer;
import dev.omatheusmesmo.demos.invoicing.domain.Invoice;
import dev.omatheusmesmo.demos.invoicing.domain.InvoiceLine;
import dev.omatheusmesmo.demos.invoicing.rules.DiscountRuleEngine;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class InvoiceService {

    @PersistenceContext(unitName = "invoicing")
    EntityManager em;

    @Inject
    DiscountRuleEngine rules;

    public Invoice create(CreateInvoiceRequest request) {
        List<Customer> found = em.createNamedQuery("Customer.byCode", Customer.class)
                .setParameter("code", request.getCustomerCode())
                .getResultList();
        if (found.isEmpty()) {
            throw new UnknownCustomerException(request.getCustomerCode());
        }
        Customer customer = found.get(0);

        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        invoice.setIssueDate(Dates.parse(request.getIssueDate()));
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CreateInvoiceRequest.Line line : request.getLines()) {
            InvoiceLine invoiceLine = new InvoiceLine(line.getSku(), line.getQuantity(), line.getUnitPrice());
            invoice.addLine(invoiceLine);
            subtotal = subtotal.add(invoiceLine.amount());
        }
        BigDecimal discount = rules.discountFor(customer.getTier(), subtotal);
        invoice.setSubtotal(subtotal);
        invoice.setDiscount(discount);
        invoice.setTotal(subtotal.subtract(discount));

        em.persist(invoice);
        em.flush();
        invoice.setReference(ReferenceCodec.encode(invoice.getId(), customer.getCode()));
        audit("CREATED " + invoice.getReference() + " " + invoice.getTotal());
        return invoice;
    }

    public Invoice find(Long id) {
        return em.find(Invoice.class, id);
    }

    public List<Invoice> list() {
        return em.createNamedQuery("Invoice.all", Invoice.class).getResultList();
    }

    public List<Customer> customers() {
        return em.createNamedQuery("Customer.all", Customer.class).getResultList();
    }

    private void audit(String event) {
        try {
            new AuditLog().record(event);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write the audit log", e);
        }
    }
}
