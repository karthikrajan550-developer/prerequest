package com.petrolbunk.dto;

import com.petrolbunk.entity.FuelType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Current price view for the Price Settings page. */
public class FuelPriceDto {
    public FuelType fuelType;
    public BigDecimal pricePerLitre;
    public LocalDate effectiveFrom;
    public LocalDateTime lastUpdated;

    public FuelPriceDto() {}

    public FuelPriceDto(FuelType fuelType, BigDecimal pricePerLitre,
                        LocalDate effectiveFrom, LocalDateTime lastUpdated) {
        this.fuelType = fuelType;
        this.pricePerLitre = pricePerLitre;
        this.effectiveFrom = effectiveFrom;
        this.lastUpdated = lastUpdated;
    }
}
