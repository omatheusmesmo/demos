package dev.omatheusmesmo.demos.invoicing.api;

import dev.omatheusmesmo.demos.invoicing.domain.Customer;

public class CustomerDto {

    private String code;
    private String name;
    private String tier;

    public static CustomerDto of(Customer customer) {
        CustomerDto dto = new CustomerDto();
        dto.code = customer.getCode();
        dto.name = customer.getName();
        dto.tier = customer.getTier().name();
        return dto;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getTier() {
        return tier;
    }
}
