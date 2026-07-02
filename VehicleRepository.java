package com.petrolbunk.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Add a credit (fuel-taken) entry. Amount is computed from litres * today's price. */
public class AddCreditRequest {
    @NotNull
    public Long vehicleId;
    @NotNull
    @Positive
    public BigDecimal litres;
    /** Optional; defaults to today. */
    public LocalDate entryDate;
    public String note;
}
