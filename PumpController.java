package com.petrolbunk.service;

import com.petrolbunk.dto.*;
import com.petrolbunk.entity.*;
import com.petrolbunk.repository.CustomerRepository;
import com.petrolbunk.repository.LedgerEntryRepository;
import com.petrolbunk.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepo;
    private final VehicleRepository vehicleRepo;
    private final LedgerEntryRepository ledgerRepo;
    private final PriceService priceService;

    public CustomerService(CustomerRepository customerRepo,
                           VehicleRepository vehicleRepo,
                           LedgerEntryRepository ledgerRepo,
                           PriceService priceService) {
        this.customerRepo = customerRepo;
        this.vehicleRepo = vehicleRepo;
        this.ledgerRepo = ledgerRepo;
        this.priceService = priceService;
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    /** All-customers credit report: totals per customer + grand totals. */
    public com.petrolbunk.dto.CreditReportDto getCreditReport() {
        com.petrolbunk.dto.CreditReportDto report = new com.petrolbunk.dto.CreditReportDto();
        List<com.petrolbunk.dto.CreditReportDto.Row> rows = new ArrayList<>();
        BigDecimal gCredit = BigDecimal.ZERO, gPaid = BigDecimal.ZERO, gOut = BigDecimal.ZERO;

        for (Customer c : customerRepo.findAllByOrderByCompanyNameAsc()) {
            com.petrolbunk.dto.CreditReportDto.Row row = new com.petrolbunk.dto.CreditReportDto.Row();
            row.customerId = c.getId();
            row.companyName = c.getCompanyName();
            row.contactPerson = c.getContactPerson();
            row.phone = c.getPhone();

            BigDecimal credit = BigDecimal.ZERO, paid = BigDecimal.ZERO;
            List<Vehicle> vehicles = vehicleRepo.findByCustomerOrderByVehicleNumberAsc(c);
            row.vehicleCount = vehicles.size();
            for (Vehicle v : vehicles) {
                for (LedgerEntry e : ledgerRepo.findByVehicleOrderByEntryDateAscIdAsc(v)) {
                    if (e.getEntryType() == LedgerEntry.EntryType.CREDIT) credit = credit.add(nz(e.getAmount()));
                    else paid = paid.add(nz(e.getAmount()));
                }
            }
            row.totalCredit = credit;
            row.totalPaid = paid;
            row.outstanding = credit.subtract(paid);
            gCredit = gCredit.add(credit);
            gPaid = gPaid.add(paid);
            gOut = gOut.add(row.outstanding);
            rows.add(row);
        }
        report.customers = rows;
        report.grandTotalCredit = gCredit;
        report.grandTotalPaid = gPaid;
        report.grandTotalOutstanding = gOut;
        return report;
    }

    // ----------------------------------------------------------- Customers
    public List<CustomerDto> listCustomers() {
        List<CustomerDto> result = new ArrayList<>();
        for (Customer c : customerRepo.findAllByOrderByCompanyNameAsc()) {
            result.add(toCustomerDto(c, false));
        }
        return result;
    }

    public CustomerDto getCustomer(Long id) {
        Customer c = customerRepo.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Customer not found."));
        return toCustomerDto(c, true);
    }

    @Transactional
    public CustomerDto createCustomer(CreateCustomerRequest req) {
        Customer c = new Customer(req.companyName.trim());
        c.setContactPerson(req.contactPerson);
        c.setPhone(req.phone);
        return toCustomerDto(customerRepo.save(c), true);
    }

    @Transactional
    public CustomerDto updateCustomer(Long id, CreateCustomerRequest req) {
        Customer c = customerRepo.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Customer not found."));
        c.setCompanyName(req.companyName.trim());
        c.setContactPerson(req.contactPerson);
        c.setPhone(req.phone);
        return toCustomerDto(customerRepo.save(c), true);
    }

    private CustomerDto toCustomerDto(Customer c, boolean includeVehicles) {
        CustomerDto dto = new CustomerDto();
        dto.id = c.getId();
        dto.companyName = c.getCompanyName();
        dto.contactPerson = c.getContactPerson();
        dto.phone = c.getPhone();
        BigDecimal total = BigDecimal.ZERO;
        List<VehicleDto> vehicles = new ArrayList<>();
        for (Vehicle v : vehicleRepo.findByCustomerOrderByVehicleNumberAsc(c)) {
            VehicleDto vd = toVehicleDto(v);
            total = total.add(nz(vd.balance));
            vehicles.add(vd);
        }
        dto.totalOutstanding = total;
        if (includeVehicles) dto.vehicles = vehicles;
        return dto;
    }

    // ------------------------------------------------------------ Vehicles
    @Transactional
    public VehicleDto addVehicle(CreateVehicleRequest req) {
        Customer c = customerRepo.findById(req.customerId)
                .orElseThrow(() -> new BusinessValidationException("Customer not found."));
        Vehicle v = new Vehicle(c, req.vehicleNumber.trim(), req.fuelType);
        v.setDescription(req.description);
        return toVehicleDto(vehicleRepo.save(v));
    }

    @Transactional
    public VehicleDto updateVehicle(Long id, CreateVehicleRequest req) {
        Vehicle v = vehicleRepo.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Vehicle not found."));
        v.setVehicleNumber(req.vehicleNumber.trim());
        v.setFuelType(req.fuelType);
        v.setDescription(req.description);
        return toVehicleDto(vehicleRepo.save(v));
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer c = customerRepo.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Customer not found."));
        // Remove all vehicles and their ledger entries first (no orphans).
        for (Vehicle v : vehicleRepo.findByCustomerOrderByVehicleNumberAsc(c)) {
            for (LedgerEntry e : ledgerRepo.findByVehicleOrderByEntryDateAscIdAsc(v)) {
                ledgerRepo.delete(e);
            }
            vehicleRepo.delete(v);
        }
        customerRepo.delete(c);
    }

    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle v = vehicleRepo.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Vehicle not found."));
        for (LedgerEntry e : ledgerRepo.findByVehicleOrderByEntryDateAscIdAsc(v)) {
            ledgerRepo.delete(e);
        }
        vehicleRepo.delete(v);
    }

    private VehicleDto toVehicleDto(Vehicle v) {
        VehicleDto dto = new VehicleDto();
        dto.id = v.getId();
        dto.customerId = v.getCustomer().getId();
        dto.vehicleNumber = v.getVehicleNumber();
        dto.fuelType = v.getFuelType();
        dto.description = v.getDescription();
        dto.currentPrice = priceService.priceFor(v.getFuelType(), LocalDate.now());

        BigDecimal credit = BigDecimal.ZERO, paid = BigDecimal.ZERO;
        for (LedgerEntry e : ledgerRepo.findByVehicleOrderByEntryDateAscIdAsc(v)) {
            if (e.getEntryType() == LedgerEntry.EntryType.CREDIT) credit = credit.add(nz(e.getAmount()));
            else paid = paid.add(nz(e.getAmount()));
        }
        dto.totalCredit = credit;
        dto.totalPaid = paid;
        dto.balance = credit.subtract(paid);
        return dto;
    }

    // -------------------------------------------------------------- Ledger
    public VehicleLedgerDto getVehicleLedger(Long vehicleId) {
        Vehicle v = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new BusinessValidationException("Vehicle not found."));
        VehicleLedgerDto dto = new VehicleLedgerDto();
        dto.vehicle = toVehicleDto(v);

        List<LedgerEntryDto> entries = new ArrayList<>();
        BigDecimal running = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO, paid = BigDecimal.ZERO;
        for (LedgerEntry e : ledgerRepo.findByVehicleOrderByEntryDateAscIdAsc(v)) {
            if (e.getEntryType() == LedgerEntry.EntryType.CREDIT) {
                running = running.add(nz(e.getAmount()));
                credit = credit.add(nz(e.getAmount()));
            } else {
                running = running.subtract(nz(e.getAmount()));
                paid = paid.add(nz(e.getAmount()));
            }
            entries.add(toEntryDto(e, running));
        }
        dto.entries = entries;
        dto.totalCredit = credit;
        dto.totalPaid = paid;
        dto.balance = credit.subtract(paid);
        return dto;
    }

    @Transactional
    public VehicleLedgerDto addCredit(AddCreditRequest req) {
        Vehicle v = vehicleRepo.findById(req.vehicleId)
                .orElseThrow(() -> new BusinessValidationException("Vehicle not found."));
        LocalDate date = req.entryDate != null ? req.entryDate : LocalDate.now();
        BigDecimal price = priceService.priceFor(v.getFuelType(), date);
        BigDecimal litres = req.litres.setScale(2, RoundingMode.HALF_UP);
        BigDecimal amount = litres.multiply(price).setScale(2, RoundingMode.HALF_UP);

        LedgerEntry e = new LedgerEntry();
        e.setVehicle(v);
        e.setEntryType(LedgerEntry.EntryType.CREDIT);
        e.setEntryDate(date);
        e.setLitres(litres);
        e.setPricePerLitre(price);
        e.setAmount(amount);
        e.setNote(req.note);
        ledgerRepo.save(e);
        return getVehicleLedger(v.getId());
    }

    @Transactional
    public VehicleLedgerDto addPayment(AddPaymentRequest req) {
        Vehicle v = vehicleRepo.findById(req.vehicleId)
                .orElseThrow(() -> new BusinessValidationException("Vehicle not found."));
        LocalDate date = req.entryDate != null ? req.entryDate : LocalDate.now();

        LedgerEntry e = new LedgerEntry();
        e.setVehicle(v);
        e.setEntryType(LedgerEntry.EntryType.PAYMENT);
        e.setEntryDate(date);
        e.setAmount(req.amount.setScale(2, RoundingMode.HALF_UP));
        e.setNote(req.note);
        ledgerRepo.save(e);
        return getVehicleLedger(v.getId());
    }

    @Transactional
    public VehicleLedgerDto updateEntry(Long entryId, UpdateEntryRequest req) {
        LedgerEntry e = ledgerRepo.findById(entryId)
                .orElseThrow(() -> new BusinessValidationException("Entry not found."));
        if (req.entryDate != null) e.setEntryDate(req.entryDate);
        if (e.getEntryType() == LedgerEntry.EntryType.CREDIT) {
            if (req.litres != null) {
                BigDecimal litres = req.litres.setScale(2, RoundingMode.HALF_UP);
                BigDecimal price = nz(e.getPricePerLitre());
                e.setLitres(litres);
                e.setAmount(litres.multiply(price).setScale(2, RoundingMode.HALF_UP));
            }
        } else {
            if (req.amount != null) {
                e.setAmount(req.amount.setScale(2, RoundingMode.HALF_UP));
            }
        }
        if (req.note != null) e.setNote(req.note);
        ledgerRepo.save(e);
        return getVehicleLedger(e.getVehicle().getId());
    }

    @Transactional
    public VehicleLedgerDto deleteEntry(Long entryId) {
        LedgerEntry e = ledgerRepo.findById(entryId)
                .orElseThrow(() -> new BusinessValidationException("Entry not found."));
        Long vehicleId = e.getVehicle().getId();
        ledgerRepo.delete(e);
        return getVehicleLedger(vehicleId);
    }

    private LedgerEntryDto toEntryDto(LedgerEntry e, BigDecimal running) {
        LedgerEntryDto dto = new LedgerEntryDto();
        dto.id = e.getId();
        dto.vehicleId = e.getVehicle().getId();
        dto.entryType = e.getEntryType().name();
        dto.entryDate = e.getEntryDate();
        dto.litres = e.getLitres();
        dto.pricePerLitre = e.getPricePerLitre();
        dto.amount = e.getAmount();
        dto.note = e.getNote();
        dto.runningBalance = running;
        return dto;
    }
}
