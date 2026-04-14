package com.smartpark.service.pricing;

import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Strategy tính phí cho xe máy: 5.000đ/giờ (tối thiểu 1 giờ).
 */
@Component
public class MotorbikePricingStrategy implements PricingStrategy {

    private static final long RATE_PER_HOUR = 5_000L;

    @Override
    public String getVehicleType() {
        return "xe_may";
    }

    @Override
    public long calculate(LocalDateTime checkIn, LocalDateTime checkOut) {
        double hours = Math.max(Duration.between(checkIn, checkOut).toMinutes() / 60.0, 1.0);
        return Math.round(hours * RATE_PER_HOUR);
    }
}
