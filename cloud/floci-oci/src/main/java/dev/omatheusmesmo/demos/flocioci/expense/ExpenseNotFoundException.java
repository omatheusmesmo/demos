package dev.omatheusmesmo.demos.flocioci.expense;

import java.util.UUID;

public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(UUID id) {
        super("Expense " + id + " not found");
    }
}
