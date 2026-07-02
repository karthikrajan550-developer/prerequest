package com.petrolbunk.dto;

import com.petrolbunk.entity.FuelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request to create or rename a pump. */
public class CreatePumpRequest {
    @NotBlank
    public String name;
    @NotNull
    public FuelType fuelType;
}
