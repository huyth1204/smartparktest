package com.smartpark.service.pricing;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory Pattern: Chọn PricingStrategy phù hợp theo loại xe.
 * Spring tự inject tất cả bean PricingStrategy vào constructor.
 */
@Component
public class PricingFactory {

    private final Map<String, PricingStrategy> strategies;

    /** Spring tự inject danh sách tất cả PricingStrategy beans */
    public PricingFactory(List<PricingStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(PricingStrategy::getVehicleType, Function.identity()));
    }

    /**
     * Trả về strategy tương ứng với vehicleType.
     * @throws IllegalArgumentException nếu không tìm thấy strategy
     */
    public PricingStrategy getStrategy(String vehicleType) {
        PricingStrategy strategy = strategies.get(vehicleType);
        if (strategy == null) {
            throw new IllegalArgumentException("Không hỗ trợ loại xe: " + vehicleType);
        }
        return strategy;
    }
}
