package com.petrolbunk.service;

import com.petrolbunk.dto.*;
import com.petrolbunk.entity.DailyReading;
import com.petrolbunk.entity.FuelType;
import com.petrolbunk.entity.Pump;
import com.petrolbunk.repository.DailyReadingRepository;
import com.petrolbunk.repository.PumpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class ReadingService {

    private final PumpRepository pumpRepository;
    private final DailyReadingRepository readingRepository;
    private final PriceService priceService;

    public ReadingService(PumpRepository pumpRepository,
                          DailyReadingRepository readingRepository,
                          PriceService priceService) {
        this.pumpRepository = pumpRepository;
        this.readingRepository = readingRepository;
        this.priceService = priceService;
    }

    /**
     * Build the day's view for every pump. For each pump:
     *  - if a reading already exists for the date, return it,
     *  - otherwise create a transient line with the opening auto-filled from the
     *    most recent prior reading's closing (0 if none yet).
     */
    public DailySummaryDto getDay(LocalDate date) {
        List<Pump> pumps = pumpRepository.findAllByOrderByDisplayOrderAsc();
        List<PumpReadingDto> lines = new ArrayList<>();

        for (Pump pump : pumps) {
            PumpReadingDto line = new PumpReadingDto();
            line.pumpId = pump.getId();
            line.pumpName = pump.getName();
            line.fuelType = pump.getFuelType();
            line.displayOrder = pump.getDisplayOrder();
            line.pricePerLitre = priceService.priceFor(pump.getFuelType(), date);

            Optional<DailyReading> existing =
                    readingRepository.findByPumpAndReadingDate(pump, date);

            if (existing.isPresent()) {
                DailyReading dr = existing.get();
                line.openingReading = dr.getOpeningReading();
                line.closingReading = dr.getClosingReading();
                line.litresSold = dr.getLitresSold();
                line.amount = dr.getAmount();
                if (dr.getPricePerLitreUsed() != null) {
                    line.pricePerLitre = dr.getPricePerLitreUsed();
                }
            } else {
                line.openingReading = openingFor(pump, date);
                line.closingReading = null;
                line.litresSold = null;
                line.amount = null;
            }
            lines.add(line);
        }

        return buildSummary(date, lines);
    }

    /** Opening reading = most recent prior day's closing for this pump (else 0). */
    private BigDecimal openingFor(Pump pump, LocalDate date) {
        return readingRepository.findMostRecentBefore(pump, date)
                .map(DailyReading::getClosingReading)
                .filter(Objects::nonNull)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Save closing readings for a date. Validates closing >= opening, computes
     * litres and amount using the price effective on that date, and persists.
     */
    @Transactional
    public DailySummaryDto saveDay(SaveReadingsRequest req) {
        LocalDate date = req.date;
        Map<Long, Pump> pumpsById = new HashMap<>();
        for (Pump p : pumpRepository.findAll()) {
            pumpsById.put(p.getId(), p);
        }

        for (SaveReadingsRequest.Entry entry : req.readings) {
            Pump pump = pumpsById.get(entry.pumpId);
            if (pump == null) {
                throw new BusinessValidationException("Unknown pump id: " + entry.pumpId);
            }
            if (entry.closingReading == null) {
                continue; // skip pumps left blank
            }

            BigDecimal opening = openingFor(pump, date);
            // If a reading already exists for the day, keep its opening (stable).
            Optional<DailyReading> existing =
                    readingRepository.findByPumpAndReadingDate(pump, date);
            DailyReading dr = existing.orElseGet(DailyReading::new);
            if (existing.isPresent() && dr.getOpeningReading() != null) {
                opening = dr.getOpeningReading();
            }

            if (entry.closingReading.compareTo(opening) < 0) {
                throw new BusinessValidationException(
                        "Closing reading (" + entry.closingReading + ") cannot be less than opening reading ("
                                + opening + ") for " + pump.getName() + ".");
            }

            BigDecimal price = priceService.priceFor(pump.getFuelType(), date);
            BigDecimal litres = entry.closingReading.subtract(opening)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal amount = litres.multiply(price).setScale(2, RoundingMode.HALF_UP);

            dr.setPump(pump);
            dr.setReadingDate(date);
            dr.setOpeningReading(opening);
            dr.setClosingReading(entry.closingReading);
            dr.setLitresSold(litres);
            dr.setPricePerLitreUsed(price);
            dr.setAmount(amount);

            readingRepository.save(dr);
        }

        return getDay(date);
    }

    /** History list: every date with readings and its grand totals. */
    public List<DateTotalDto> getHistory() {
        List<DateTotalDto> result = new ArrayList<>();
        for (LocalDate date : readingRepository.findDistinctDates()) {
            result.add(dateTotalFor(date));
        }
        return result;
    }

    /** Grand totals for a single date. */
    private DateTotalDto dateTotalFor(LocalDate date) {
        List<DailyReading> readings = readingRepository.findByReadingDate(date);
        BigDecimal litres = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;
        for (DailyReading dr : readings) {
            if (dr.getLitresSold() != null) litres = litres.add(dr.getLitresSold());
            if (dr.getAmount() != null) amount = amount.add(dr.getAmount());
        }
        return new DateTotalDto(date, litres, amount);
    }

    /**
     * Monthly aggregation: per-fuel-type totals and grand totals for a calendar
     * month, plus the list of days (each with its own totals) for the
     * expandable daily rows. If year/month are 0, defaults to the current month.
     */
    public MonthlySummaryDto getMonth(int year, int month) {
        LocalDate ref = (year == 0 || month == 0) ? LocalDate.now() : LocalDate.of(year, month, 1);
        LocalDate from = ref.withDayOfMonth(1);
        LocalDate to = ref.withDayOfMonth(ref.lengthOfMonth());

        List<DailyReading> readings = readingRepository.findByDateRange(from, to);

        Map<FuelType, BigDecimal> litresByType = new EnumMap<>(FuelType.class);
        Map<FuelType, BigDecimal> amountByType = new EnumMap<>(FuelType.class);
        // Per-day accumulation, preserving date order.
        Map<LocalDate, BigDecimal> dayLitres = new TreeMap<>();
        Map<LocalDate, BigDecimal> dayAmount = new TreeMap<>();
        BigDecimal grandLitres = BigDecimal.ZERO;
        BigDecimal grandAmount = BigDecimal.ZERO;

        for (DailyReading dr : readings) {
            BigDecimal litres = dr.getLitresSold() != null ? dr.getLitresSold() : BigDecimal.ZERO;
            BigDecimal amount = dr.getAmount() != null ? dr.getAmount() : BigDecimal.ZERO;
            FuelType type = dr.getPump().getFuelType();
            litresByType.merge(type, litres, BigDecimal::add);
            amountByType.merge(type, amount, BigDecimal::add);
            dayLitres.merge(dr.getReadingDate(), litres, BigDecimal::add);
            dayAmount.merge(dr.getReadingDate(), amount, BigDecimal::add);
            grandLitres = grandLitres.add(litres);
            grandAmount = grandAmount.add(amount);
        }

        MonthlySummaryDto dto = new MonthlySummaryDto();
        dto.year = ref.getYear();
        dto.month = ref.getMonthValue();
        dto.monthLabel = ref.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + ref.getYear();

        List<FuelTypeTotalDto> totals = new ArrayList<>();
        for (FuelType type : FuelType.values()) {
            totals.add(new FuelTypeTotalDto(
                    type,
                    litresByType.getOrDefault(type, BigDecimal.ZERO),
                    amountByType.getOrDefault(type, BigDecimal.ZERO)));
        }
        dto.fuelTypeTotals = totals;
        dto.grandTotalLitres = grandLitres;
        dto.grandTotalAmount = grandAmount;

        List<DateTotalDto> days = new ArrayList<>();
        // Newest day first for display.
        List<LocalDate> dates = new ArrayList<>(dayLitres.keySet());
        Collections.reverse(dates);
        for (LocalDate d : dates) {
            days.add(new DateTotalDto(d, dayLitres.get(d), dayAmount.get(d)));
        }
        dto.days = days;
        return dto;
    }

    /** Assemble per-fuel subtotals and grand totals from the pump lines. */
    private DailySummaryDto buildSummary(LocalDate date, List<PumpReadingDto> lines) {
        DailySummaryDto summary = new DailySummaryDto();
        summary.date = date;
        summary.pumps = lines;

        Map<FuelType, BigDecimal> litresByType = new EnumMap<>(FuelType.class);
        Map<FuelType, BigDecimal> amountByType = new EnumMap<>(FuelType.class);
        BigDecimal grandLitres = BigDecimal.ZERO;
        BigDecimal grandAmount = BigDecimal.ZERO;

        for (PumpReadingDto line : lines) {
            BigDecimal litres = line.litresSold != null ? line.litresSold : BigDecimal.ZERO;
            BigDecimal amount = line.amount != null ? line.amount : BigDecimal.ZERO;
            litresByType.merge(line.fuelType, litres, BigDecimal::add);
            amountByType.merge(line.fuelType, amount, BigDecimal::add);
            grandLitres = grandLitres.add(litres);
            grandAmount = grandAmount.add(amount);
        }

        List<FuelTypeTotalDto> totals = new ArrayList<>();
        for (FuelType type : FuelType.values()) {
            totals.add(new FuelTypeTotalDto(
                    type,
                    litresByType.getOrDefault(type, BigDecimal.ZERO),
                    amountByType.getOrDefault(type, BigDecimal.ZERO)));
        }

        summary.fuelTypeTotals = totals;
        summary.grandTotalLitres = grandLitres;
        summary.grandTotalAmount = grandAmount;
        return summary;
    }
}
