package dev.omatheusmesmo.demos.flocioci.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record PlaceOrderRequest(
        @NotBlank String customer,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal total,
        @NotNull @Pattern(regexp = "\\d{16}", message = "must be 16 digits") String cardNumber) {
}
