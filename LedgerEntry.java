package com.petrolbunk.service;

import com.petrolbunk.dto.FuelPriceDto;
import com.petrolbunk.dto.UpdatePriceRequest;
import com.petrolbunk.entity.FuelPrice;
import com.petrolbunk.entity.FuelType;
import com.petrolbunk.repository.FuelPriceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PriceService {

    private final FuelPriceRepository priceRepository;

    public PriceService(FuelPriceRepository priceRepository) {
        this.priceRepository = priceRepository;
    }

    /** Current price for every fuel type (latest row), for the settings page. */
    public List<FuelPriceDto> getCurrentPrices() {
        List<FuelPriceDto> result = new ArrayList<>();
        for (FuelType type : FuelType.values()) {
            Optional<FuelPrice> current = priceRepository.findCurrent(type);
            current.ifPresent(fp -> result.add(new FuelPriceDto(
                    fp.getFuelType(), fp.getPricePerLitre(),
                    fp.getEffectiveFrom(), fp.getCreatedAt())));
        }
        return result;
    }

    /**
     * Set or edit a price. This always inserts a new history row so past days
     * keep their original price. effectiveFrom defaults to today.
     */
    @Transactional
    public FuelPriceDto updatePrice(UpdatePriceRequest req) {
        LocalDate effectiveFrom = req.effectiveFrom != null ? req.effectiveFrom : LocalDate.now();
        FuelPrice fp = new FuelPrice(req.fuelType, req.pricePerLitre, effectiveFrom);
        FuelPrice saved = priceRepository.save(fp);
        return new FuelPriceDto(saved.getFuelType(), saved.getPricePerLitre(),
                saved.getEffectiveFrom(), saved.getCreatedAt());
    }

    /** Price effective for a fuel type on a date; 0 if none configured. */
    public BigDecimal priceFor(FuelType fuelType, LocalDate date) {
        // Prefer the price effective on the given date; if none is found for that
        // date (e.g. entry date is before the first price row), fall back to the
        // latest known price so the amount is never wrongly zero.
        return priceRepository.findEffectivePrice(fuelType, date)
                .or(() -> priceRepository.findCurrent(fuelType))
                .map(FuelPrice::getPricePerLitre)
                .orElse(BigDecimal.ZERO);
    }
}
