package com.petrolbunk.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row per pump per day. Stores the opening and closing meter readings.
 *
 * openingReading is auto-populated from the previous day's closingReading for
 * the same pump (handled in the service layer). litresSold and amount are
 * computed and stored at save time using the price effective on readingDate.
 */
@Entity
@Table(
    name = "daily_reading",
    uniqueConstraints = @UniqueConstraint(columnNames = {"pump_id", "readingDate"})
)
public class DailyReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "pump_id", nullable = false)
    private Pump pump;

    @Column(nullable = false)
    private LocalDate readingDate;

    /** Cumulative meter value at start of day. Auto-filled from prior day's close. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal openingReading;

    /** Cumulative meter value at end of day. Entered by the user. */
    @Column(precision = 12, scale = 2)
    private BigDecimal closingReading;

    /** closingReading - openingReading. Computed at save time. */
    @Column(precision = 12, scale = 2)
    private BigDecimal litresSold;

    /** litresSold * price effective on readingDate. Computed at save time. */
    @Column(precision = 14, scale = 2)
    private BigDecimal amount;

    /** Price per litre used for this row (snapshot of the effective price). */
    @Column(precision = 10, scale = 2)
    private BigDecimal pricePerLitreUsed;

    public DailyReading() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Pump getPump() {
        return pump;
    }

    public void setPump(Pump pump) {
        this.pump = pump;
    }

    public LocalDate getReadingDate() {
        return readingDate;
    }

    public void setReadingDate(LocalDate readingDate) {
        this.readingDate = readingDate;
    }

    public BigDecimal getOpeningReading() {
        return openingReading;
    }

    public void setOpeningReading(BigDecimal openingReading) {
        this.openingReading = openingReading;
    }

    public BigDecimal getClosingReading() {
        return closingReading;
    }

    public void setClosingReading(BigDecimal closingReading) {
        this.closingReading = closingReading;
    }

    public BigDecimal getLitresSold() {
        return litresSold;
    }

    public void setLitresSold(BigDecimal litresSold) {
        this.litresSold = litresSold;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPricePerLitreUsed() {
        return pricePerLitreUsed;
    }

    public void setPricePerLitreUsed(BigDecimal pricePerLitreUsed) {
        this.pricePerLitreUsed = pricePerLitreUsed;
    }
}
