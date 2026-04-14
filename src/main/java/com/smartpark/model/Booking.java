package com.smartpark.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity @Table(name = "bookings")
@Data @NoArgsConstructor
public class Booking {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;
    private String licensePlate;
    private String vehicleType;       // xe_may / o_to

    private LocalDateTime checkIn;
    private LocalDateTime checkOut;

    private String status;            // PENDING / CONFIRMED / PAID
    private String paymentCode;       // mã khách nhập khi chuyển khoản
    private Long   amountDue;

    private LocalDateTime paidAt;
    private String bankRef;
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }
}
