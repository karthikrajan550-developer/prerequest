package com.petrolbunk.dto;

import java.math.BigDecimal;
import java.util.List;

/** All-customers credit summary: each customer's totals + grand total owed. */
public class CreditReportDto {
    public List<Row> customers;
    public BigDecimal grandTotalCredit;
    public BigDecimal grandTotalPaid;
    public BigDecimal grandTotalOutstanding;

    public CreditReportDto() {}

    public static class Row {
        public Long customerId;
        public String companyName;
        public String contactPerson;
        public String phone;
        public int vehicleCount;
        public BigDecimal totalCredit;
        public BigDecimal totalPaid;
        public BigDecimal outstanding;
    }
}
