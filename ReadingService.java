package com.petrolbunk.repository;

import com.petrolbunk.entity.DailyReading;
import com.petrolbunk.entity.Pump;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyReadingRepository extends JpaRepository<DailyReading, Long> {

    List<DailyReading> findByReadingDate(LocalDate readingDate);

    Optional<DailyReading> findByPumpAndReadingDate(Pump pump, LocalDate readingDate);

    /**
     * The most recent reading for a pump strictly before the given date.
     * Used to auto-populate the opening reading.
     */
    @Query("SELECT dr FROM DailyReading dr " +
           "WHERE dr.pump = :pump AND dr.readingDate < :date " +
           "ORDER BY dr.readingDate DESC")
    List<DailyReading> findPreviousReadings(@Param("pump") Pump pump,
                                            @Param("date") LocalDate date);

    default Optional<DailyReading> findMostRecentBefore(Pump pump, LocalDate date) {
        List<DailyReading> list = findPreviousReadings(pump, date);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Distinct dates that have any readings, newest first (for the history list). */
    @Query("SELECT DISTINCT dr.readingDate FROM DailyReading dr ORDER BY dr.readingDate DESC")
    List<LocalDate> findDistinctDates();

    /** All readings within an inclusive date range (for monthly aggregation). */
    @Query("SELECT dr FROM DailyReading dr " +
           "WHERE dr.readingDate >= :from AND dr.readingDate <= :to " +
           "ORDER BY dr.readingDate ASC")
    List<DailyReading> findByDateRange(@Param("from") LocalDate from,
                                       @Param("to") LocalDate to);
}
