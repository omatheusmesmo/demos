package dev.omatheusmesmo.demos.invoicing.export;

import dev.omatheusmesmo.demos.invoicing.domain.Invoice;
import dev.omatheusmesmo.demos.invoicing.domain.InvoiceLine;
import dev.omatheusmesmo.demos.invoicing.service.Dates;
import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

public final class InvoiceXmlExporter {

    private static final JAXBContext CONTEXT = createContext();

    private InvoiceXmlExporter() {
    }

    public static String toXml(Invoice invoice) {
        InvoiceXml xml = new InvoiceXml();
        xml.reference = invoice.getReference();
        xml.customer = invoice.getCustomer().getCode();
        xml.issueDate = Dates.format(invoice.getIssueDate());
        for (InvoiceLine line : invoice.getLines()) {
            InvoiceXml.Line l = new InvoiceXml.Line();
            l.sku = line.getSku();
            l.quantity = line.getQuantity();
            l.unitPrice = line.getUnitPrice();
            l.amount = line.amount();
            xml.lines.add(l);
        }
        xml.subtotal = invoice.getSubtotal();
        xml.discount = invoice.getDiscount();
        xml.total = invoice.getTotal();
        try {
            Marshaller marshaller = CONTEXT.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            StringWriter out = new StringWriter();
            marshaller.marshal(xml, out);
            return out.toString();
        } catch (JAXBException e) {
            throw new IllegalStateException("Cannot export invoice " + invoice.getId(), e);
        }
    }

    private static JAXBContext createContext() {
        try {
            return JAXBContext.newInstance(InvoiceXml.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
