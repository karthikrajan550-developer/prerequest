package com.petrolbunk.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** A credit customer, identified by company name. Has many vehicles. */
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName;

    /** Optional contact info. */
    private String contactPerson;
    private String phone;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Customer() {
    }

    public Customer(String companyName) {
        this.companyName = companyName;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
