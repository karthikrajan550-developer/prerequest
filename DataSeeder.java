package com.petrolbunk.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Full payload for a day: each pump line, per-fuel subtotals, and grand totals. */
public class DailySummaryDto {
    public LocalDate date;
    public List<PumpReadingDto> pumps;
    public List<FuelTypeTotalDto> fuelTypeTotals;
    public BigDecimal grandTotalLitres;
    public BigDecimal grandTotalAmount;

    public DailySummaryDto() {}
}
