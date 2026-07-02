package com.petrolbunk.repository;

import com.petrolbunk.entity.LedgerEntry;
import com.petrolbunk.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByVehicleOrderByEntryDateAscIdAsc(Vehicle vehicle);
    List<LedgerEntry> findByVehicleId(Long vehicleId);
}
