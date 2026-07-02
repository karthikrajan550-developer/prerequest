package com.petrolbunk.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Request to save closing readings for a day. */
public class SaveReadingsRequest {
    @NotNull
    public LocalDate date;

    @NotNull
    public List<Entry> readings;

    public static class Entry {
        public Long pumpId;
        public BigDecimal closingReading;
    }
}
