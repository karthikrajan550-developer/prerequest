package com.petrolbunk.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Row in the history list: a date and its grand totals. */
public class DateTotalDto {
    public LocalDate date;
    public BigDecimal grandTotalLitres;
    public BigDecimal grandTotalAmount;

    public DateTotalDto() {}

    public DateTotalDto(LocalDate date, BigDecimal grandTotalLitres, BigDecimal grandTotalAmount) {
        this.date = date;
        this.grandTotalLitres = grandTotalLitres;
        this.grandTotalAmount = grandTotalAmount;
    }
}
