package com.smartpark;

import com.smartpark.model.ParkingSlot;
import com.smartpark.model.StaffAccount;
import com.smartpark.repository.ParkingSlotRepository;
import com.smartpark.repository.StaffAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ParkingSlotRepository slotRepo;
    private final StaffAccountRepository staffRepo;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataInitializer(ParkingSlotRepository slotRepo,
                           StaffAccountRepository staffRepo,
                           BCryptPasswordEncoder passwordEncoder) {
        this.slotRepo        = slotRepo;
        this.staffRepo       = staffRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (slotRepo.count() == 0) {
            String[] motoRows = {"A","B","C","D","E"};
            for (String row : motoRows)
                for (int col = 1; col <= 10; col++)
                    slotRepo.save(new ParkingSlot("M-" + row + col, "motorbike"));

            String[] carRows = {"A","B","C"};
            for (String row : carRows)
                for (int col = 1; col <= 6; col++)
                    slotRepo.save(new ParkingSlot("C-" + row + col, "car"));
        }

        if (staffRepo.count() == 0) {
            staffRepo.save(new StaffAccount("AD001", "Admin Hệ Thống",  "admin",     passwordEncoder.encode("admin123"), "admin"));
            staffRepo.save(new StaffAccount("NV001", "Nguyễn Văn An",   "nhanvien1", passwordEncoder.encode("123456"),   "staff"));
            staffRepo.save(new StaffAccount("NV002", "Trần Thị Bình",   "nhanvien2", passwordEncoder.encode("123456"),   "staff"));
            staffRepo.save(new StaffAccount("NV003", "Lê Minh Cường",   "nhanvien3", passwordEncoder.encode("123456"),   "staff"));
        }
    }
}
