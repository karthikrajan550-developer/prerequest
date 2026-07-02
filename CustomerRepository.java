package com.petrolbunk.dto;

import java.math.BigDecimal;
import java.util.List;

/** Customer with summary balance and (optionally) its vehicles. */
public class CustomerDto {
    public Long id;
    public String companyName;
    public String contactPerson;
    public String phone;
    public BigDecimal totalOutstanding;   // sum of all vehicles' balances
    public List<VehicleDto> vehicles;

    public CustomerDto() {}
}
