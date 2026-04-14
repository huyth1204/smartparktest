package com.smartpark.repository;

import com.smartpark.model.ParkingSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, String> {
    List<ParkingSlot> findByZone(String zone);
    List<ParkingSlot> findByZoneAndOccupied(String zone, boolean occupied);
}
