package com.petrolbunk.repository;

import com.petrolbunk.entity.Pump;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PumpRepository extends JpaRepository<Pump, Long> {
    List<Pump> findAllByOrderByDisplayOrderAsc();
}
