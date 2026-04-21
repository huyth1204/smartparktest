package com.smartpark.service.pricing;

import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Strategy tính phí cho xe máy: 15.000đ/12 giờ (tối thiểu 1 lượt).
 */
@Component
public class MotorbikePricingStrategy implements PricingStrategy {

    private static final long RATE_PER_SLOT = 15_000L;
    private static final double HOURS_PER_SLOT = 12.0;

    @Override
    public String getVehicleType() {
        return "xe_may";
    }

    @Override
    public long calculate(LocalDateTime checkIn, LocalDateTime checkOut) {
        double totalHours = Duration.between(checkIn, checkOut).toMinutes() / 60.0;
        long slots = Math.max((long) Math.ceil(totalHours / HOURS_PER_SLOT), 1);
        return slots * RATE_PER_SLOT;
    }
}
