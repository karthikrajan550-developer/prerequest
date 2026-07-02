package com.petrolbunk.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Record a payment/settlement against a vehicle's balance. */
public class AddPaymentRequest {
    @NotNull
    public Long vehicleId;
    @NotNull
    @Positive
    public BigDecimal amount;
    public LocalDate entryDate;   // defaults to today
    public String note;
}
