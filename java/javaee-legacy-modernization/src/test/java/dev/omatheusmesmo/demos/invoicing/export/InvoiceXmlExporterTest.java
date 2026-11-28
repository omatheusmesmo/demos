package dev.omatheusmesmo.demos.invoicing.export;

import static org.junit.Assert.assertEquals;

import dev.omatheusmesmo.demos.invoicing.domain.Customer;
import dev.omatheusmesmo.demos.invoicing.domain.CustomerTier;
import dev.omatheusmesmo.demos.invoicing.domain.Invoice;
import dev.omatheusmesmo.demos.invoicing.domain.InvoiceLine;
import dev.omatheusmesmo.demos.invoicing.service.Dates;
import java.math.BigDecimal;
import org.junit.Test;

public class InvoiceXmlExporterTest {

    @Test
    public void exportsAnInvoiceAsXml() {
        Invoice invoice = new Invoice();
        invoice.setId(7L);
        invoice.setReference("SU5WLTctQUNNRQ==");
        invoice.setCustomer(new Customer("ACME", "Acme Corporation", CustomerTier.GOLD));
        invoice.setIssueDate(Dates.parse("2026-12-01"));
        invoice.addLine(new InvoiceLine("WIDGET", 3, new BigDecimal("10.50")));
        invoice.setSubtotal(new BigDecimal("31.50"));
        invoice.setDiscount(new BigDecimal("3.15"));
        invoice.setTotal(new BigDecimal("28.35"));

        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<invoice reference=\"SU5WLTctQUNNRQ==\">\n"
                + "    <customer>ACME</customer>\n"
                + "    <issueDate>2026-12-01</issueDate>\n"
                + "    <lines>\n"
                + "        <line sku=\"WIDGET\" quantity=\"3\" unitPrice=\"10.50\" amount=\"31.50\"/>\n"
                + "    </lines>\n"
                + "    <subtotal>31.50</subtotal>\n"
                + "    <discount>3.15</discount>\n"
                + "    <total>28.35</total>\n"
                + "</invoice>\n";
        assertEquals(expected, InvoiceXmlExporter.toXml(invoice));
    }
}
