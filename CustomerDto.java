package com.petrolbunk.dto;

import com.petrolbunk.entity.FuelType;

public class PumpDto {
    public Long id;
    public String name;
    public FuelType fuelType;
    public int displayOrder;

    public PumpDto() {}

    public PumpDto(Long id, String name, FuelType fuelType, int displayOrder) {
        this.id = id;
        this.name = name;
        this.fuelType = fuelType;
        this.displayOrder = displayOrder;
    }
}
