package com.smartpark.service;

import com.smartpark.model.ParkingSlot;

import java.util.List;

/**
 * Service interface quản lý bãi đỗ xe.
 *
 * Thể hiện tính Trừu tượng (Abstraction) và Dependency Inversion Principle (DIP):
 * - Controller chỉ phụ thuộc vào interface này, không biết implementation cụ thể.
 * - Dễ swap implementation (vd: đổi từ H2 sang Redis cache) mà không sửa controller.
 */
public interface ParkingService {

    List<ParkingSlot> getAllSlots();

    List<ParkingSlot> getSlotsByZone(String zone);

    /** Thống kê tổng hợp — trả về SlotStats record */
    ParkingSlotService.SlotStats getStats();

    /** Ghi nhận xe vào — ném exception nếu ô đã có xe */
    ParkingSlot checkin(String slotId, String licensePlate);

    /** Ghi nhận xe ra — ném exception nếu ô đang trống */
    ParkingSlot checkout(String slotId);

    List<ParkingSlot> searchByPlate(String plate);

    void reserveSlot(String slotId, String licensePlate);

    void resetAllSlots();
}
