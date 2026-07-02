package com.petrolbunk.dto;

import com.petrolbunk.entity.FuelType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Request body to set/edit a fuel price. */
public class UpdatePriceRequest {
    @NotNull
    public FuelType fuelType;

    @NotNull
    @Positive
    public BigDecimal pricePerLitre;

    /** Optional: date the new price takes effect. Defaults to today if null. */
    public LocalDate effectiveFrom;
}
