package com.petrolbunk.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateCustomerRequest {
    @NotBlank
    public String companyName;
    public String contactPerson;
    public String phone;
}
