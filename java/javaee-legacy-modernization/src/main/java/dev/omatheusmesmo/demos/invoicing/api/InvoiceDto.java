package dev.omatheusmesmo.demos.invoicing.api;

import dev.omatheusmesmo.demos.invoicing.domain.Invoice;
import dev.omatheusmesmo.demos.invoicing.domain.InvoiceLine;
import dev.omatheusmesmo.demos.invoicing.service.Dates;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDto {

    private Long id;
    private String reference;
    private String customerCode;
    private String issueDate;
    private List<LineDto> lines = new ArrayList<LineDto>();
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;

    public static InvoiceDto of(Invoice invoice) {
        InvoiceDto dto = new InvoiceDto();
        dto.id = invoice.getId();
        dto.reference = invoice.getReference();
        dto.customerCode = invoice.getCustomer().getCode();
        dto.issueDate = Dates.format(invoice.getIssueDate());
        for (InvoiceLine line : invoice.getLines()) {
            dto.lines.add(LineDto.of(line));
        }
        dto.subtotal = invoice.getSubtotal();
        dto.discount = invoice.getDiscount();
        dto.total = invoice.getTotal();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public String getIssueDate() {
        return issueDate;
    }

    public List<LineDto> getLines() {
        return lines;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public static class LineDto {

        private String sku;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;

        static LineDto of(InvoiceLine line) {
            LineDto dto = new LineDto();
            dto.sku = line.getSku();
            dto.quantity = line.getQuantity();
            dto.unitPrice = line.getUnitPrice();
            dto.amount = line.amount();
            return dto;
        }

        public String getSku() {
            return sku;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }
}
