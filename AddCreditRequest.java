package com.petrolbunk.dto;

import com.petrolbunk.entity.FuelType;
import java.math.BigDecimal;

/** One pump's reading line on the Daily Entry / Summary page. */
public class PumpReadingDto {
    public Long pumpId;
    public String pumpName;
    public FuelType fuelType;
    public int displayOrder;
    public BigDecimal openingReading;   // auto-filled from previous day
    public BigDecimal closingReading;   // entered by user (may be null until entered)
    public BigDecimal litresSold;       // computed
    public BigDecimal pricePerLitre;    // effective price for the day
    public BigDecimal amount;           // computed

    public PumpReadingDto() {}
}
