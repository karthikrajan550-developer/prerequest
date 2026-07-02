package com.petrolbunk.service;

import com.petrolbunk.dto.DailySummaryDto;
import com.petrolbunk.dto.FuelTypeTotalDto;
import com.petrolbunk.dto.PumpReadingDto;
import com.petrolbunk.dto.CustomerDto;
import com.petrolbunk.dto.VehicleDto;
import com.petrolbunk.dto.VehicleLedgerDto;
import com.petrolbunk.dto.LedgerEntryDto;
import com.petrolbunk.dto.CreditReportDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds Excel (.xlsx) and PDF exports for a date range. Data comes from
 * ReadingService.getDay(), so the exported numbers exactly match what is stored
 * and shown in the app — nothing is recalculated here.
 */
@Service
public class ExportService {

    private final ReadingService readingService;
    private final CustomerService customerService;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    // "Rs." is used in PDF because the default PDF fonts don't render the ₹ glyph.
    private static final String RUPEE_PDF = "Rs. ";

    public ExportService(ReadingService readingService, CustomerService customerService) {
        this.readingService = readingService;
        this.customerService = customerService;
    }

    private List<DailySummaryDto> collectRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessValidationException("Both 'from' and 'to' dates are required.");
        }
        if (to.isBefore(from)) {
            throw new BusinessValidationException("'to' date cannot be before 'from' date.");
        }
        List<DailySummaryDto> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            days.add(readingService.getDay(d));
        }
        return days;
    }

    private String fuelLabel(String fuelType) {
        if ("POWER_PETROL".equals(fuelType)) return "Power Petrol";
        return fuelType.charAt(0) + fuelType.substring(1).toLowerCase();
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    // ---------------------------------------------------------------- EXCEL
    public byte[] buildExcel(LocalDate from, LocalDate to) {
        List<DailySummaryDto> days = collectRange(from, to);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle title = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            title.setFont(titleFont);

            CellStyle header = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            header.setFont(headerFont);
            header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setBorderBottom(BorderStyle.THIN);

            CellStyle bold = wb.createCellStyle();
            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            bold.setFont(boldFont);

            CellStyle money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            CellStyle moneyBold = wb.createCellStyle();
            moneyBold.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            moneyBold.setFont(boldFont);

            // ----- Summary sheet: one row per day -----
            Sheet summary = wb.createSheet("Summary");
            int r = 0;
            Row tRow = summary.createRow(r++);
            Cell tCell = tRow.createCell(0);
            tCell.setCellValue("Nandha Agencies - HP  |  Sales Summary  "
                    + from.format(DF) + " to " + to.format(DF));
            tCell.setCellStyle(title);
            r++; // blank line

            String[] sumCols = {"Date", "Petrol (L)", "Diesel (L)", "Power (L)",
                    "Total Litres", "Total Amount"};
            Row sh = summary.createRow(r++);
            for (int c = 0; c < sumCols.length; c++) {
                Cell cell = sh.createCell(c);
                cell.setCellValue(sumCols[c]);
                cell.setCellStyle(header);
            }

            BigDecimal grandLitres = BigDecimal.ZERO;
            BigDecimal grandAmount = BigDecimal.ZERO;

            for (DailySummaryDto day : days) {
                Row row = summary.createRow(r++);
                row.createCell(0).setCellValue(day.date.format(DF));
                BigDecimal petrol = BigDecimal.ZERO, diesel = BigDecimal.ZERO, power = BigDecimal.ZERO;
                for (FuelTypeTotalDto t : day.fuelTypeTotals) {
                    if ("PETROL".equals(t.fuelType.name())) petrol = nz(t.totalLitres);
                    else if ("DIESEL".equals(t.fuelType.name())) diesel = nz(t.totalLitres);
                    else power = nz(t.totalLitres);
                }
                setNum(row, 1, petrol, money);
                setNum(row, 2, diesel, money);
                setNum(row, 3, power, money);
                setNum(row, 4, nz(day.grandTotalLitres), money);
                setNum(row, 5, nz(day.grandTotalAmount), money);
                grandLitres = grandLitres.add(nz(day.grandTotalLitres));
                grandAmount = grandAmount.add(nz(day.grandTotalAmount));
            }

            Row totalRow = summary.createRow(r++);
            Cell gLabel = totalRow.createCell(0);
            gLabel.setCellValue("GRAND TOTAL");
            gLabel.setCellStyle(bold);
            setNum(totalRow, 4, grandLitres, moneyBold);
            setNum(totalRow, 5, grandAmount, moneyBold);

            for (int c = 0; c < sumCols.length; c++) summary.autoSizeColumn(c);

            // ----- One detail sheet per day (pump-by-pump) -----
            for (DailySummaryDto day : days) {
                Sheet sheet = wb.createSheet(day.date.format(DF));
                int dr = 0;
                Row dTitle = sheet.createRow(dr++);
                Cell dt = dTitle.createCell(0);
                dt.setCellValue("Nandha Agencies - HP  |  " + day.date.format(DF));
                dt.setCellStyle(title);
                dr++;

                String[] cols = {"Pump", "Fuel Type", "Opening", "Closing",
                        "Litres Sold", "Price/Litre", "Amount"};
                Row hr = sheet.createRow(dr++);
                for (int c = 0; c < cols.length; c++) {
                    Cell cell = hr.createCell(c);
                    cell.setCellValue(cols[c]);
                    cell.setCellStyle(header);
                }

                for (PumpReadingDto p : day.pumps) {
                    Row row = sheet.createRow(dr++);
                    row.createCell(0).setCellValue(p.pumpName);
                    row.createCell(1).setCellValue(fuelLabel(p.fuelType.name()));
                    setNum(row, 2, nz(p.openingReading), money);
                    setNum(row, 3, nz(p.closingReading), money);
                    setNum(row, 4, nz(p.litresSold), money);
                    setNum(row, 5, nz(p.pricePerLitre), money);
                    setNum(row, 6, nz(p.amount), money);
                }

                Row dTotal = sheet.createRow(dr++);
                Cell dLabel = dTotal.createCell(0);
                dLabel.setCellValue("TOTAL");
                dLabel.setCellStyle(bold);
                setNum(dTotal, 4, nz(day.grandTotalLitres), moneyBold);
                setNum(dTotal, 6, nz(day.grandTotalAmount), moneyBold);

                for (int c = 0; c < cols.length; c++) sheet.autoSizeColumn(c);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Excel export: " + e.getMessage(), e);
        }
    }

    private void setNum(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    // ------------------------------------------------------------------ PDF
    public byte[] buildPdf(LocalDate from, LocalDate to) {
        List<DailySummaryDto> days = collectRange(from, to);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 42, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            com.lowagie.text.Font h1 = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD, new java.awt.Color(10, 61, 98));
            com.lowagie.text.Font h2 = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD, new java.awt.Color(10, 61, 98));
            com.lowagie.text.Font small = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, new java.awt.Color(90, 90, 90));
            com.lowagie.text.Font cellFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font cellBold = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font whiteBold = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, java.awt.Color.WHITE);

            Paragraph title = new Paragraph("Nandha Agencies - HP", h1);
            doc.add(title);
            Paragraph sub = new Paragraph("Sales Report: "
                    + from.format(DF) + "  to  " + to.format(DF), small);
            sub.setSpacingAfter(14);
            doc.add(sub);

            // ----- Range summary table -----
            doc.add(new Paragraph("Summary by day", h2));
            PdfPTable sumTable = new PdfPTable(new float[]{2.2f, 2f, 2.4f});
            sumTable.setWidthPercentage(100);
            sumTable.setSpacingBefore(6);
            sumTable.setSpacingAfter(16);
            addHeaderCell(sumTable, "Date", whiteBold);
            addHeaderCell(sumTable, "Total Litres", whiteBold);
            addHeaderCell(sumTable, "Total Amount", whiteBold);

            BigDecimal grandLitres = BigDecimal.ZERO;
            BigDecimal grandAmount = BigDecimal.ZERO;
            for (DailySummaryDto day : days) {
                addCell(sumTable, day.date.format(DF), cellFont, Element.ALIGN_LEFT);
                addCell(sumTable, fmt(nz(day.grandTotalLitres)), cellFont, Element.ALIGN_RIGHT);
                addCell(sumTable, RUPEE_PDF + fmt(nz(day.grandTotalAmount)), cellFont, Element.ALIGN_RIGHT);
                grandLitres = grandLitres.add(nz(day.grandTotalLitres));
                grandAmount = grandAmount.add(nz(day.grandTotalAmount));
            }
            addCell(sumTable, "GRAND TOTAL", cellBold, Element.ALIGN_LEFT);
            addCell(sumTable, fmt(grandLitres), cellBold, Element.ALIGN_RIGHT);
            addCell(sumTable, RUPEE_PDF + fmt(grandAmount), cellBold, Element.ALIGN_RIGHT);
            doc.add(sumTable);

            // ----- Per-day detail tables -----
            for (DailySummaryDto day : days) {
                doc.add(new Paragraph(day.date.format(DF), h2));
                PdfPTable t = new PdfPTable(new float[]{2f, 1.6f, 1.6f, 1.6f, 1.4f, 1.4f, 1.8f});
                t.setWidthPercentage(100);
                t.setSpacingBefore(6);
                t.setSpacingAfter(16);
                for (String col : new String[]{"Pump", "Opening", "Closing", "Litres",
                        "Price/L", "Fuel", "Amount"}) {
                    addHeaderCell(t, col, whiteBold);
                }
                for (PumpReadingDto p : day.pumps) {
                    addCell(t, p.pumpName, cellFont, Element.ALIGN_LEFT);
                    addCell(t, fmt(nz(p.openingReading)), cellFont, Element.ALIGN_RIGHT);
                    addCell(t, fmt(nz(p.closingReading)), cellFont, Element.ALIGN_RIGHT);
                    addCell(t, fmt(nz(p.litresSold)), cellFont, Element.ALIGN_RIGHT);
                    addCell(t, fmt(nz(p.pricePerLitre)), cellFont, Element.ALIGN_RIGHT);
                    addCell(t, fuelLabel(p.fuelType.name()), cellFont, Element.ALIGN_LEFT);
                    addCell(t, RUPEE_PDF + fmt(nz(p.amount)), cellFont, Element.ALIGN_RIGHT);
                }
                addCell(t, "TOTAL", cellBold, Element.ALIGN_LEFT);
                addCell(t, "", cellFont, Element.ALIGN_RIGHT);
                addCell(t, "", cellFont, Element.ALIGN_RIGHT);
                addCell(t, fmt(nz(day.grandTotalLitres)), cellBold, Element.ALIGN_RIGHT);
                addCell(t, "", cellFont, Element.ALIGN_RIGHT);
                addCell(t, "", cellFont, Element.ALIGN_LEFT);
                addCell(t, RUPEE_PDF + fmt(nz(day.grandTotalAmount)), cellBold, Element.ALIGN_RIGHT);
                doc.add(t);
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build PDF export: " + e.getMessage(), e);
        }
    }

    private void addHeaderCell(PdfPTable table, String text, com.lowagie.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new java.awt.Color(10, 61, 98));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, com.lowagie.text.Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(4);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private String fmt(BigDecimal v) {
        return String.format("%,.2f", v);
    }

    // =================================================== CUSTOMER STATEMENT
    public byte[] buildCustomerExcel(Long customerId) {
        CustomerDto customer = customerService.getCustomer(customerId);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle title = wb.createCellStyle();
            Font tf = wb.createFont(); tf.setBold(true); tf.setFontHeightInPoints((short) 14);
            title.setFont(tf);
            CellStyle header = wb.createCellStyle();
            Font hf = wb.createFont(); hf.setBold(true); hf.setColor(IndexedColors.WHITE.getIndex());
            header.setFont(hf);
            header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle bold = wb.createCellStyle();
            Font bf = wb.createFont(); bf.setBold(true); bold.setFont(bf);
            CellStyle money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            CellStyle moneyBold = wb.createCellStyle();
            moneyBold.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            moneyBold.setFont(bf);

            Sheet overview = wb.createSheet("Overview");
            int r = 0;
            Row t = overview.createRow(r++);
            Cell tc = t.createCell(0);
            tc.setCellValue("Nandha Agencies - HP  |  Customer Statement: " + customer.companyName);
            tc.setCellStyle(title);
            r++;
            String[] cols = {"Vehicle", "Fuel Type", "Total Credit", "Total Paid", "Balance"};
            Row hr = overview.createRow(r++);
            for (int c = 0; c < cols.length; c++) { Cell cell = hr.createCell(c); cell.setCellValue(cols[c]); cell.setCellStyle(header); }

            if (customer.vehicles != null) {
                for (VehicleDto v : customer.vehicles) {
                    Row row = overview.createRow(r++);
                    row.createCell(0).setCellValue(v.vehicleNumber);
                    row.createCell(1).setCellValue(fuelLabel(v.fuelType.name()));
                    setNum(row, 2, nz(v.totalCredit), money);
                    setNum(row, 3, nz(v.totalPaid), money);
                    setNum(row, 4, nz(v.balance), money);
                }
            }
            Row totRow = overview.createRow(r++);
            Cell tl = totRow.createCell(0); tl.setCellValue("TOTAL OUTSTANDING"); tl.setCellStyle(bold);
            setNum(totRow, 4, nz(customer.totalOutstanding), moneyBold);
            for (int c = 0; c < cols.length; c++) overview.autoSizeColumn(c);

            // One sheet per vehicle: full ledger
            if (customer.vehicles != null) {
                for (VehicleDto v : customer.vehicles) {
                    VehicleLedgerDto led = customerService.getVehicleLedger(v.id);
                    String safe = v.vehicleNumber.replaceAll("[\\\\/*?:\\[\\]]", "-");
                    if (safe.length() > 28) safe = safe.substring(0, 28);
                    Sheet sh = wb.createSheet(safe);
                    int vr = 0;
                    Row vt = sh.createRow(vr++);
                    Cell vtc = vt.createCell(0);
                    vtc.setCellValue(v.vehicleNumber + "  (" + fuelLabel(v.fuelType.name()) + ")");
                    vtc.setCellStyle(title);
                    vr++;
                    String[] lc = {"Date", "Type", "Litres", "Price/L", "Amount", "Balance", "Note"};
                    Row lh = sh.createRow(vr++);
                    for (int c = 0; c < lc.length; c++) { Cell cell = lh.createCell(c); cell.setCellValue(lc[c]); cell.setCellStyle(header); }
                    for (LedgerEntryDto e : led.entries) {
                        Row row = sh.createRow(vr++);
                        row.createCell(0).setCellValue(e.entryDate.format(DF));
                        row.createCell(1).setCellValue(e.entryType);
                        if (e.litres != null) setNum(row, 2, e.litres, money);
                        if (e.pricePerLitre != null) setNum(row, 3, e.pricePerLitre, money);
                        setNum(row, 4, nz(e.amount), money);
                        setNum(row, 5, nz(e.runningBalance), money);
                        row.createCell(6).setCellValue(e.note == null ? "" : e.note);
                    }
                    Row bRow = sh.createRow(vr++);
                    Cell bl = bRow.createCell(0); bl.setCellValue("BALANCE"); bl.setCellStyle(bold);
                    setNum(bRow, 5, nz(led.balance), moneyBold);
                    for (int c = 0; c < lc.length; c++) sh.autoSizeColumn(c);
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build customer Excel: " + e.getMessage(), e);
        }
    }

    public byte[] buildCustomerPdf(Long customerId) {
        CustomerDto customer = customerService.getCustomer(customerId);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 42, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();
            com.lowagie.text.Font h1 = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD, new java.awt.Color(10, 61, 98));
            com.lowagie.text.Font h2 = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD, new java.awt.Color(10, 61, 98));
            com.lowagie.text.Font small = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, new java.awt.Color(90, 90, 90));
            com.lowagie.text.Font cellFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font cellBold = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font whiteBold = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, java.awt.Color.WHITE);

            doc.add(new Paragraph("Nandha Agencies - HP", h1));
            Paragraph sub = new Paragraph("Customer Statement: " + customer.companyName, small);
            sub.setSpacingAfter(4);
            doc.add(sub);
            Paragraph outstanding = new Paragraph(
                    "Total outstanding: " + RUPEE_PDF + fmt(nz(customer.totalOutstanding)), h2);
            outstanding.setSpacingAfter(14);
            doc.add(outstanding);

            if (customer.vehicles != null) {
                for (VehicleDto v : customer.vehicles) {
                    VehicleLedgerDto led = customerService.getVehicleLedger(v.id);
                    doc.add(new Paragraph(v.vehicleNumber + "  (" + fuelLabel(v.fuelType.name()) + ")", h2));
                    PdfPTable tbl = new PdfPTable(new float[]{1.6f, 1.2f, 1.2f, 1.2f, 1.6f, 1.6f, 1.8f});
                    tbl.setWidthPercentage(100);
                    tbl.setSpacingBefore(6);
                    tbl.setSpacingAfter(14);
                    for (String col : new String[]{"Date", "Type", "Litres", "Price/L", "Amount", "Balance", "Note"}) {
                        addHeaderCell(tbl, col, whiteBold);
                    }
                    for (LedgerEntryDto e : led.entries) {
                        addCell(tbl, e.entryDate.format(DF), cellFont, Element.ALIGN_LEFT);
                        addCell(tbl, e.entryType, cellFont, Element.ALIGN_LEFT);
                        addCell(tbl, e.litres == null ? "-" : fmt(e.litres), cellFont, Element.ALIGN_RIGHT);
                        addCell(tbl, e.pricePerLitre == null ? "-" : fmt(e.pricePerLitre), cellFont, Element.ALIGN_RIGHT);
                        addCell(tbl, RUPEE_PDF + fmt(nz(e.amount)), cellFont, Element.ALIGN_RIGHT);
                        addCell(tbl, RUPEE_PDF + fmt(nz(e.runningBalance)), cellFont, Element.ALIGN_RIGHT);
                        addCell(tbl, e.note == null ? "" : e.note, cellFont, Element.ALIGN_LEFT);
                    }
                    addCell(tbl, "BALANCE", cellBold, Element.ALIGN_LEFT);
                    addCell(tbl, "", cellFont, Element.ALIGN_LEFT);
                    addCell(tbl, "", cellFont, Element.ALIGN_RIGHT);
                    addCell(tbl, "", cellFont, Element.ALIGN_RIGHT);
                    addCell(tbl, "", cellFont, Element.ALIGN_RIGHT);
                    addCell(tbl, RUPEE_PDF + fmt(nz(led.balance)), cellBold, Element.ALIGN_RIGHT);
                    addCell(tbl, "", cellFont, Element.ALIGN_LEFT);
                    doc.add(tbl);
                }
            }
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build customer PDF: " + e.getMessage(), e);
        }
    }

    // ============================================ ALL-CUSTOMERS CREDIT REPORT
    public byte[] buildCreditReportExcel() {
        CreditReportDto rep = customerService.getCreditReport();
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle title = wb.createCellStyle();
            Font tf = wb.createFont(); tf.setBold(true); tf.setFontHeightInPoints((short) 14);
            title.setFont(tf);
            CellStyle header = wb.createCellStyle();
            Font hf = wb.createFont(); hf.setBold(true); hf.setColor(IndexedColors.WHITE.getIndex());
            header.setFont(hf);
            header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle bold = wb.createCellStyle();
            Font bf = wb.createFont(); bf.setBold(true); bold.setFont(bf);
            CellStyle money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            CellStyle moneyBold = wb.createCellStyle();
            moneyBold.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            moneyBold.setFont(bf);

            Sheet sh = wb.createSheet("Credit Report");
            int r = 0;
            Row t = sh.createRow(r++);
            Cell tc = t.createCell(0);
            tc.setCellValue("Nandha Agencies - HP  |  Customer Credit Report");
            tc.setCellStyle(title);
            r++;

            String[] cols = {"Company", "Contact", "Phone", "Vehicles",
                    "Total Credit", "Total Paid", "Outstanding"};
            Row hr = sh.createRow(r++);
            for (int c = 0; c < cols.length; c++) { Cell cell = hr.createCell(c); cell.setCellValue(cols[c]); cell.setCellStyle(header); }

            for (CreditReportDto.Row row : rep.customers) {
                Row xr = sh.createRow(r++);
                xr.createCell(0).setCellValue(row.companyName);
                xr.createCell(1).setCellValue(row.contactPerson == null ? "" : row.contactPerson);
                xr.createCell(2).setCellValue(row.phone == null ? "" : row.phone);
                xr.createCell(3).setCellValue(row.vehicleCount);
                setNum(xr, 4, nz(row.totalCredit), money);
                setNum(xr, 5, nz(row.totalPaid), money);
                setNum(xr, 6, nz(row.outstanding), money);
            }

            Row g = sh.createRow(r++);
            Cell gl = g.createCell(0); gl.setCellValue("GRAND TOTAL"); gl.setCellStyle(bold);
            setNum(g, 4, nz(rep.grandTotalCredit), moneyBold);
            setNum(g, 5, nz(rep.grandTotalPaid), moneyBold);
            setNum(g, 6, nz(rep.grandTotalOutstanding), moneyBold);

            for (int c = 0; c < cols.length; c++) sh.autoSizeColumn(c);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build credit report Excel: " + e.getMessage(), e);
        }
    }

    public byte[] buildCreditReportPdf() {
        CreditReportDto rep = customerService.getCreditReport();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 42, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();
            com.lowagie.text.Font h1 = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD, new java.awt.Color(10, 61, 98));
            com.lowagie.text.Font h2 = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD, new java.awt.Color(10, 61, 98));
            com.lowagie.text.Font cellFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font cellBold = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font whiteBold = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, java.awt.Color.WHITE);

            doc.add(new Paragraph("Nandha Agencies - HP", h1));
            Paragraph sub = new Paragraph("Customer Credit Report", h2);
            sub.setSpacingAfter(4);
            doc.add(sub);
            Paragraph tot = new Paragraph(
                    "Total outstanding across all customers: " + RUPEE_PDF + fmt(nz(rep.grandTotalOutstanding)), h2);
            tot.setSpacingAfter(14);
            doc.add(tot);

            PdfPTable tbl = new PdfPTable(new float[]{2.4f, 1.8f, 1.8f, 1f, 1.8f, 1.8f, 2f});
            tbl.setWidthPercentage(100);
            for (String col : new String[]{"Company", "Contact", "Phone", "Veh.",
                    "Credit", "Paid", "Outstanding"}) {
                addHeaderCell(tbl, col, whiteBold);
            }
            for (CreditReportDto.Row row : rep.customers) {
                addCell(tbl, row.companyName, cellFont, Element.ALIGN_LEFT);
                addCell(tbl, row.contactPerson == null ? "-" : row.contactPerson, cellFont, Element.ALIGN_LEFT);
                addCell(tbl, row.phone == null ? "-" : row.phone, cellFont, Element.ALIGN_LEFT);
                addCell(tbl, String.valueOf(row.vehicleCount), cellFont, Element.ALIGN_RIGHT);
                addCell(tbl, RUPEE_PDF + fmt(nz(row.totalCredit)), cellFont, Element.ALIGN_RIGHT);
                addCell(tbl, RUPEE_PDF + fmt(nz(row.totalPaid)), cellFont, Element.ALIGN_RIGHT);
                addCell(tbl, RUPEE_PDF + fmt(nz(row.outstanding)), cellFont, Element.ALIGN_RIGHT);
            }
            addCell(tbl, "GRAND TOTAL", cellBold, Element.ALIGN_LEFT);
            addCell(tbl, "", cellFont, Element.ALIGN_LEFT);
            addCell(tbl, "", cellFont, Element.ALIGN_LEFT);
            addCell(tbl, "", cellFont, Element.ALIGN_RIGHT);
            addCell(tbl, RUPEE_PDF + fmt(nz(rep.grandTotalCredit)), cellBold, Element.ALIGN_RIGHT);
            addCell(tbl, RUPEE_PDF + fmt(nz(rep.grandTotalPaid)), cellBold, Element.ALIGN_RIGHT);
            addCell(tbl, RUPEE_PDF + fmt(nz(rep.grandTotalOutstanding)), cellBold, Element.ALIGN_RIGHT);
            doc.add(tbl);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build credit report PDF: " + e.getMessage(), e);
        }
    }
}
