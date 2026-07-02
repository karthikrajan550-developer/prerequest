package com.petrolbunk.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated view for a calendar month: per-fuel-type totals across the whole
 * month, a grand total, and the list of individual days (each with its own
 * totals) so the UI can expand a month into its daily rows.
 */
public class MonthlySummaryDto {
    public int year;
    public int month;                 // 1-12
    public String monthLabel;         // e.g. "June 2026"
    public List<FuelTypeTotalDto> fuelTypeTotals;
    public BigDecimal grandTotalLitres;
    public BigDecimal grandTotalAmount;
    public List<DateTotalDto> days;   // each day in the month that has data

    public MonthlySummaryDto() {}
}
