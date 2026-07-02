package com.petrolbunk.dto;

import com.petrolbunk.entity.FuelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateVehicleRequest {
    @NotNull
    public Long customerId;
    @NotBlank
    public String vehicleNumber;
    @NotNull
    public FuelType fuelType;
    public String description;
}
