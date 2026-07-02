package com.petrolbunk.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Edit an existing ledger entry. For CREDIT, litres re-computes amount at the
 *  stored price; for PAYMENT, amount is used directly. */
public class UpdateEntryRequest {
    public LocalDate entryDate;
    public BigDecimal litres;   // credit only
    public BigDecimal amount;   // payment only
    public String note;
}
