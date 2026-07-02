package com.petrolbunk.dto;

import com.petrolbunk.entity.FuelType;
import java.math.BigDecimal;

/** Subtotal for one fuel type on a day. */
public class FuelTypeTotalDto {
    public FuelType fuelType;
    public BigDecimal totalLitres;
    public BigDecimal totalAmount;

    public FuelTypeTotalDto() {}

    public FuelTypeTotalDto(FuelType fuelType, BigDecimal totalLitres, BigDecimal totalAmount) {
        this.fuelType = fuelType;
        this.totalLitres = totalLitres;
        this.totalAmount = totalAmount;
    }
}
