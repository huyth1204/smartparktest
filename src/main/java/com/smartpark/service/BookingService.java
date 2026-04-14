package com.smartpark.service;

import com.smartpark.model.Booking;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface quản lý đặt vé / booking.
 * Tách interface từ implementation — thể hiện Abstraction + Dependency Inversion.
 */
public interface BookingService {

    Booking createBooking(String customerName, String plate,
                          String vehicleType,
                          LocalDateTime checkIn, LocalDateTime checkOut);

    boolean processPayment(String content, long amount, String bankRef);

    List<Booking> getAll();

    Optional<Booking> findById(Long id);

    void deleteAll();
}
