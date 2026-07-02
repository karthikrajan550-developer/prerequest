package com.petrolbunk.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single ledger line for a vehicle. Two kinds:
 *  - CREDIT: fuel taken on credit. litres and pricePerLitre set; amount = litres*price.
 *  - PAYMENT: a repayment/settlement. amount set directly; litres/price null.
 * The vehicle's outstanding balance = sum(CREDIT amounts) - sum(PAYMENT amounts).
 */
@Entity
@Table(name = "ledger_entry")
public class LedgerEntry {

    public enum EntryType { CREDIT, PAYMENT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType entryType;

    @Column(nullable = false)
    private LocalDate entryDate;

    /** For CREDIT entries: litres of fuel taken. Null for payments. */
    @Column(precision = 12, scale = 2)
    private BigDecimal litres;

    /** For CREDIT entries: price per litre used (snapshot of that day's price). */
    @Column(precision = 10, scale = 2)
    private BigDecimal pricePerLitre;

    /** Amount in INR. Credit = litres*price; payment = amount paid. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /** Optional note (e.g. "cash", "cheque no. 123"). */
    private String note;

    public LedgerEntry() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
    public EntryType getEntryType() { return entryType; }
    public void setEntryType(EntryType entryType) { this.entryType = entryType; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
    public BigDecimal getLitres() { return litres; }
    public void setLitres(BigDecimal litres) { this.litres = litres; }
    public BigDecimal getPricePerLitre() { return pricePerLitre; }
    public void setPricePerLitre(BigDecimal pricePerLitre) { this.pricePerLitre = pricePerLitre; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
