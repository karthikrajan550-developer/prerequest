package com.petrolbunk.dto;

import com.petrolbunk.entity.FuelType;
import java.math.BigDecimal;

/** Vehicle with its running balance. */
public class VehicleDto {
    public Long id;
    public Long customerId;
    public String vehicleNumber;
    public FuelType fuelType;
    public String description;
    public BigDecimal totalCredit;    // sum of credit amounts
    public BigDecimal totalPaid;      // sum of payments
    public BigDecimal balance;        // credit - paid (outstanding)
    public BigDecimal currentPrice;   // today's price for this fuel type

    public VehicleDto() {}
}
