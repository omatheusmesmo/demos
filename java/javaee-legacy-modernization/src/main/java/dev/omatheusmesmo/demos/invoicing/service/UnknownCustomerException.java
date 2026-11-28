package dev.omatheusmesmo.demos.invoicing.service;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class UnknownCustomerException extends RuntimeException {

    public UnknownCustomerException(String code) {
        super("Unknown customer: " + code);
    }
}
