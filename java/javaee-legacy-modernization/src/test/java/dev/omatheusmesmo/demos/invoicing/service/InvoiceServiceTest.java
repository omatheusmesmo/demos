package dev.omatheusmesmo.demos.invoicing.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.omatheusmesmo.demos.invoicing.api.CreateInvoiceRequest;
import dev.omatheusmesmo.demos.invoicing.audit.AuditLog;
import dev.omatheusmesmo.demos.invoicing.domain.Customer;
import dev.omatheusmesmo.demos.invoicing.domain.CustomerTier;
import dev.omatheusmesmo.demos.invoicing.domain.Invoice;
import dev.omatheusmesmo.demos.invoicing.rules.DiscountRuleEngine;
import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class InvoiceServiceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private InvoiceService service;
    private TypedQuery<Customer> byCode;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        System.setProperty(AuditLog.FILE_PROPERTY, new File(folder.getRoot(), "audit.log").getPath());
        service = new InvoiceService();
        service.em = mock(EntityManager.class);
        service.rules = mock(DiscountRuleEngine.class);
        byCode = mock(TypedQuery.class);
        when(service.em.createNamedQuery("Customer.byCode", Customer.class)).thenReturn(byCode);
        when(byCode.setParameter(eq("code"), anyString())).thenReturn(byCode);
        doAnswer(invocation -> {
            ((Invoice) invocation.getArguments()[0]).setId(5L);
            return null;
        }).when(service.em).persist(any(Invoice.class));
    }

    @Test
    public void createsAnInvoiceWithTheDiscountFromTheRules() {
        when(byCode.getResultList()).thenReturn(Arrays.asList(new Customer("ACME", "Acme", CustomerTier.GOLD)));
        when(service.rules.discountFor(CustomerTier.GOLD, new BigDecimal("41.00"))).thenReturn(new BigDecimal("4.10"));

        Invoice invoice = service.create(request("ACME",
                new CreateInvoiceRequest.Line("WIDGET", 2, new BigDecimal("10.50")),
                new CreateInvoiceRequest.Line("GADGET", 1, new BigDecimal("20.00"))));

        assertEquals(new BigDecimal("41.00"), invoice.getSubtotal());
        assertEquals(new BigDecimal("4.10"), invoice.getDiscount());
        assertEquals(new BigDecimal("36.90"), invoice.getTotal());
        assertEquals(ReferenceCodec.encode(5L, "ACME"), invoice.getReference());
        verify(service.em).persist(invoice);
    }

    @Test(expected = UnknownCustomerException.class)
    public void rejectsAnUnknownCustomer() {
        when(byCode.getResultList()).thenReturn(Collections.<Customer>emptyList());
        try {
            service.create(request("NOBODY", new CreateInvoiceRequest.Line("WIDGET", 1, BigDecimal.ONE)));
        } finally {
            verify(service.em, never()).persist(any());
        }
    }

    private static CreateInvoiceRequest request(String customer, CreateInvoiceRequest.Line... lines) {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setCustomerCode(customer);
        request.setIssueDate("2026-12-01");
        request.setLines(Arrays.asList(lines));
        return request;
    }
}
