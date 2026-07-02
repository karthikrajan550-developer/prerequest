package com.petrolbunk.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One ledger line for display, with running balance after this entry. */
public class LedgerEntryDto {
    public Long id;
    public Long vehicleId;
    public String entryType;        // CREDIT or PAYMENT
    public LocalDate entryDate;
    public BigDecimal litres;       // null for payments
    public BigDecimal pricePerLitre;// null for payments
    public BigDecimal amount;
    public String note;
    public BigDecimal runningBalance; // balance after this entry

    public LedgerEntryDto() {}
}
