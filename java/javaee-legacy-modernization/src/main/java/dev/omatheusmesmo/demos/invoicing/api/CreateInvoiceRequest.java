package dev.omatheusmesmo.demos.invoicing.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CreateInvoiceRequest {

    private String customerCode;
    private String issueDate;
    private List<Line> lines = new ArrayList<Line>();

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public String getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(String issueDate) {
        this.issueDate = issueDate;
    }

    public List<Line> getLines() {
        return lines;
    }

    public void setLines(List<Line> lines) {
        this.lines = lines;
    }

    public static class Line {

        private String sku;
        private int quantity;
        private BigDecimal unitPrice;

        public Line() {
        }

        public Line(String sku, int quantity, BigDecimal unitPrice) {
            this.sku = sku;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }
    }
}
