package com.smartpark.service;

import com.smartpark.exception.ResourceNotFoundException;
import com.smartpark.exception.SlotAlreadyOccupiedException;
import com.smartpark.exception.SlotNotOccupiedException;
import com.smartpark.model.ParkingSlot;
import com.smartpark.repository.ParkingSlotRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Service quản lý ô đỗ xe.
 * Implements ParkingService — thể hiện Abstraction + Dependency Inversion (DIP).
 * Controller chỉ phụ thuộc vào interface, không biết implementation cụ thể.
 */
@Primary
@Service
public class ParkingSlotService implements ParkingService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ParkingSlotRepository repo;

    // Constructor injection — best practice Spring, dễ test (không cần @Autowired)
    public ParkingSlotService(ParkingSlotRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<ParkingSlot> getAllSlots() { return repo.findAll(); }

    @Override
    public List<ParkingSlot> getSlotsByZone(String zone) { return repo.findByZone(zone); }

    public Optional<ParkingSlot> findById(String id) { return repo.findById(id); }

    /**
     * Ghi nhận xe vào bãi.
     * Ném Custom Exception thay vì trả về null/error string — đúng Java idiom.
     */
    @Override
    public ParkingSlot checkin(String slotId, String licensePlate) {
        if (!StringUtils.hasText(slotId))
            throw new IllegalArgumentException("Mã ô đỗ không được trống");
        if (!StringUtils.hasText(licensePlate))
            throw new IllegalArgumentException("Biển số không được trống");

        String normalizedId = slotId.trim().toUpperCase();
        ParkingSlot slot = repo.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException("Ô đỗ", "id", normalizedId));

        if (slot.isOccupied())
            throw new SlotAlreadyOccupiedException(normalizedId);

        slot.setOccupied(true);
        slot.setLicensePlate(licensePlate.trim().toUpperCase());
        slot.setCheckinTime(LocalTime.now().format(TIME_FMT));
        return repo.save(slot);
    }

    /**
     * Ghi nhận xe ra bãi.
     */
    @Override
    public ParkingSlot checkout(String slotId) {
        if (!StringUtils.hasText(slotId))
            throw new IllegalArgumentException("Mã ô đỗ không được trống");

        String normalizedId = slotId.trim().toUpperCase();
        ParkingSlot slot = repo.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException("Ô đỗ", "id", normalizedId));

        if (!slot.isOccupied() && !slot.isReserved())
            throw new SlotNotOccupiedException(normalizedId);

        slot.setOccupied(false);
        slot.setReserved(false);
        slot.setLicensePlate(null);
        slot.setCheckinTime(null);
        return repo.save(slot);
    }

    /**
     * Tìm xe đang đỗ theo biển số (partial match, case-insensitive).
     */
    @Override
    public List<ParkingSlot> searchByPlate(String plate) {
        if (!StringUtils.hasText(plate)) return List.of();
        String q = plate.trim().toUpperCase();
        return repo.findAll().stream()
                .filter(s -> s.isOccupied()
                        && s.getLicensePlate() != null
                        && s.getLicensePlate().contains(q))
                .toList();
    }

    /**
     * Đặt trước ô đỗ — đánh dấu reserved, hiển thị đỏ trên map.
     */
    @Override
    public void reserveSlot(String slotId, String licensePlate) {
        if (!StringUtils.hasText(slotId)) return;
        String normalizedId = slotId.trim().toUpperCase();
        repo.findById(normalizedId).ifPresent(slot -> {
            if (slot.isOccupied() || slot.isReserved())
                throw new SlotAlreadyOccupiedException(normalizedId);
            slot.setReserved(true);
            slot.setLicensePlate(licensePlate != null ? licensePlate.trim().toUpperCase() : null);
            repo.save(slot);
        });
    }

    /**
     * Reset tất cả ô về trống.
     * Dùng saveAll() thay vì save() trong loop — tối ưu batch DB.
     */
    @Override
    public void resetAllSlots() {
        List<ParkingSlot> all = repo.findAll();
        all.forEach(s -> {
            s.setOccupied(false);
            s.setReserved(false);
            s.setLicensePlate(null);
            s.setCheckinTime(null);
        });
        repo.saveAll(all); // batch update — hiệu quả hơn save() từng cái
    }

    /**
     * Thống kê tổng hợp bãi xe.
     * Trả về SlotStats record — immutable DTO nội bộ.
     */
    @Override
    public SlotStats getStats() {
        List<ParkingSlot> all = repo.findAll();
        long total      = all.size();
        long filled     = all.stream().filter(s -> s.isOccupied() || s.isReserved()).count();
        long motoTotal  = all.stream().filter(s -> "motorbike".equals(s.getZone())).count();
        long motoFilled = all.stream().filter(s -> "motorbike".equals(s.getZone()) && (s.isOccupied() || s.isReserved())).count();
        long carTotal   = all.stream().filter(s -> "car".equals(s.getZone())).count();
        long carFilled  = all.stream().filter(s -> "car".equals(s.getZone()) && (s.isOccupied() || s.isReserved())).count();
        int  pct        = total > 0 ? (int) Math.round(filled * 100.0 / total) : 0;
        return new SlotStats(total, filled, total - filled,
                motoTotal, motoFilled, motoTotal - motoFilled,
                carTotal,  carFilled,  carTotal  - carFilled, pct);
    }

    /**
     * Immutable DTO thống kê — dùng Java Record (Java 16+).
     * Record tự generate constructor, getters, equals, hashCode, toString.
     */
    public record SlotStats(
            long total, long filled, long empty,
            long motoTotal, long motoFilled, long motoEmpty,
            long carTotal,  long carFilled,  long carEmpty,
            int  pct) {}
}
