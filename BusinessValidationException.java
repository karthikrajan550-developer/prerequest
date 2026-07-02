package com.petrolbunk.repository;

import com.petrolbunk.entity.Customer;
import com.petrolbunk.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByCustomerOrderByVehicleNumberAsc(Customer customer);
    List<Vehicle> findByCustomerId(Long customerId);
}
