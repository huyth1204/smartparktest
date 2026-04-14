package com.smartpark.service.pricing;

import java.time.LocalDateTime;

/**
 * Strategy Pattern: Interface tính phí gửi xe.
 * Mỗi loại xe sẽ có cách tính phí riêng.
 */
public interface PricingStrategy {

    /** Trả về mã loại xe mà strategy này xử lý (vd: "xe_may", "o_to") */
    String getVehicleType();

    /** Tính tiền dựa trên thời gian vào – ra */
    long calculate(LocalDateTime checkIn, LocalDateTime checkOut);
}
