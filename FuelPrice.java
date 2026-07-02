package com.petrolbunk.repository;

import com.petrolbunk.entity.FuelPrice;
import com.petrolbunk.entity.FuelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FuelPriceRepository extends JpaRepository<FuelPrice, Long> {

    /**
     * The price effective for a fuel type on a given date: the most recent row
     * whose effectiveFrom is on or before that date.
     */
    @Query("SELECT fp FROM FuelPrice fp " +
           "WHERE fp.fuelType = :fuelType AND fp.effectiveFrom <= :date " +
           "ORDER BY fp.effectiveFrom DESC, fp.id DESC")
    List<FuelPrice> findEffectivePrices(@Param("fuelType") FuelType fuelType,
                                        @Param("date") LocalDate date);

    default Optional<FuelPrice> findEffectivePrice(FuelType fuelType, LocalDate date) {
        List<FuelPrice> list = findEffectivePrices(fuelType, date);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Latest price row per fuel type (for the Price Settings "current" view). */
    @Query("SELECT fp FROM FuelPrice fp " +
           "WHERE fp.fuelType = :fuelType " +
           "ORDER BY fp.effectiveFrom DESC, fp.id DESC")
    List<FuelPrice> findLatestByFuelType(@Param("fuelType") FuelType fuelType);

    default Optional<FuelPrice> findCurrent(FuelType fuelType) {
        List<FuelPrice> list = findLatestByFuelType(fuelType);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
