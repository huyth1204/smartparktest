package com.smartpark.service.impl;

import com.smartpark.model.Booking;
import com.smartpark.repository.BookingRepository;
import com.smartpark.service.BookingService;
import com.smartpark.service.pricing.PricingFactory;
import com.smartpark.service.pricing.PricingStrategy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Implementation của BookingService.
 * Sử dụng PricingFactory (Factory Pattern) + PricingStrategy (Strategy Pattern)
 * để tính phí thay vì if/else trực tiếp.
 */
@Primary
@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository repo;
    private final PricingFactory pricingFactory;

    public BookingServiceImpl(BookingRepository repo, PricingFactory pricingFactory) {
        this.repo = repo;
        this.pricingFactory = pricingFactory;
    }

    @Override
    public Booking createBooking(String customerName, String plate,
                                 String vehicleType,
                                 LocalDateTime checkIn, LocalDateTime checkOut) {
        // Sử dụng Strategy Pattern: lấy strategy tương ứng loại xe
        PricingStrategy pricing = pricingFactory.getStrategy(vehicleType);

        Booking b = new Booking();
        b.setCustomerName(customerName);
        b.setLicensePlate(plate.toUpperCase().trim());
        b.setVehicleType(vehicleType);
        b.setCheckIn(checkIn);
        b.setCheckOut(checkOut);
        b.setAmountDue(pricing.calculate(checkIn, checkOut)); // Polymorphism!
        b.setPaymentCode(generatePaymentCode());
        b.setStatus("CONFIRMED");
        return repo.save(b);
    }

    @Override
    public boolean processPayment(String content, long amount, String bankRef) {
        if (content == null) return false;
        String upper = content.toUpperCase();

        Optional<Booking> found = repo.findAll().stream()
                .filter(b -> b.getPaymentCode() != null
                          && upper.contains(b.getPaymentCode())
                          && !"PAID".equals(b.getStatus()))
                .findFirst();

        if (found.isEmpty()) return false;

        Booking b = found.get();
        if (amount < b.getAmountDue()) return false;

        b.setStatus("PAID");
        b.setPaidAt(LocalDateTime.now());
        b.setBankRef(bankRef);
        repo.save(b);

        System.out.printf("[PAID] ✅ %s | %s | %,d đ%n",
                b.getPaymentCode(), b.getLicensePlate(), amount);
        return true;
    }

    @Override
    public List<Booking> getAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Optional<Booking> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public void deleteAll() {
        repo.deleteAll();
    }

    // ── Private helper ──────────────────────────────────
    private String generatePaymentCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("SP");
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
