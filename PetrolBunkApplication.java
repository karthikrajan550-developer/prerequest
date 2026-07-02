package com.petrolbunk.config;

import com.petrolbunk.entity.FuelPrice;
import com.petrolbunk.entity.FuelType;
import com.petrolbunk.entity.Pump;
import com.petrolbunk.repository.FuelPriceRepository;
import com.petrolbunk.repository.PumpRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Seeds the default pumps and sample prices on first run (only if DB is empty). */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(PumpRepository pumpRepo, FuelPriceRepository priceRepo) {
        return args -> {
            if (pumpRepo.count() == 0) {
                int order = 1;
                for (int i = 1; i <= 3; i++) {
                    pumpRepo.save(new Pump("Power-" + i, FuelType.POWER_PETROL, order++));
                }
                for (int i = 1; i <= 2; i++) {
                    pumpRepo.save(new Pump("Petrol-" + i, FuelType.PETROL, order++));
                }
                for (int i = 1; i <= 3; i++) {
                    pumpRepo.save(new Pump("Diesel-" + i, FuelType.DIESEL, order++));
                }
            }
            if (priceRepo.count() == 0) {
                LocalDate today = LocalDate.now();
                priceRepo.save(new FuelPrice(FuelType.PETROL, new BigDecimal("100.50"), today));
                priceRepo.save(new FuelPrice(FuelType.DIESEL, new BigDecimal("92.25"), today));
                priceRepo.save(new FuelPrice(FuelType.POWER_PETROL, new BigDecimal("110.75"), today));
            }
        };
    }
}
