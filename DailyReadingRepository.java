package com.petrolbunk.dto;

import java.math.BigDecimal;
import java.util.List;

/** A vehicle's full ledger: its details, all entries, and totals. */
public class VehicleLedgerDto {
    public VehicleDto vehicle;
    public List<LedgerEntryDto> entries;
    public BigDecimal totalCredit;
    public BigDecimal totalPaid;
    public BigDecimal balance;

    public VehicleLedgerDto() {}
}
