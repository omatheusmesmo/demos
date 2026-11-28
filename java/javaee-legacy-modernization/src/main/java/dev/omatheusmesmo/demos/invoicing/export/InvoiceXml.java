package dev.omatheusmesmo.demos.invoicing.export;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "invoice")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"customer", "issueDate", "lines", "subtotal", "discount", "total"})
public class InvoiceXml {

    @XmlAttribute
    String reference;

    String customer;

    String issueDate;

    @XmlElementWrapper(name = "lines")
    @XmlElement(name = "line")
    List<Line> lines = new ArrayList<Line>();

    BigDecimal subtotal;

    BigDecimal discount;

    BigDecimal total;

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Line {

        @XmlAttribute
        String sku;

        @XmlAttribute
        int quantity;

        @XmlAttribute
        BigDecimal unitPrice;

        @XmlAttribute
        BigDecimal amount;
    }
}
