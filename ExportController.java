package com.petrolbunk.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Stores a price for a fuel type that is effective starting on a given date.
 *
 * We keep a HISTORY: instead of overwriting the price, each change inserts a new
 * row with an effectiveFrom date. The price that applies to any given day is the
 * row for that fuel type with the latest effectiveFrom that is <= that day.
 * This ensures past days' sales stay calculated at the price that applied then.
 */
@Entity
@Table(name = "fuel_price")
public class FuelPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FuelType fuelType;

    /** Price per litre in INR. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerLitre;

    /** The date from which this price is effective (inclusive). */
    @Column(nullable = false)
    private LocalDate effectiveFrom;

    /** When this price row was created (for the "last updated" display). */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public FuelPrice() {
    }

    public FuelPrice(FuelType fuelType, BigDecimal pricePerLitre, LocalDate effectiveFrom) {
        this.fuelType = fuelType;
        this.pricePerLitre = pricePerLitre;
        this.effectiveFrom = effectiveFrom;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    public BigDecimal getPricePerLitre() {
        return pricePerLitre;
    }

    public void setPricePerLitre(BigDecimal pricePerLitre) {
        this.pricePerLitre = pricePerLitre;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
