package com.smartpark;

import com.smartpark.model.Booking;
import com.smartpark.model.ParkingSlot;
import com.smartpark.model.StaffAccount;
import com.smartpark.repository.BookingRepository;
import com.smartpark.repository.ParkingSlotRepository;
import com.smartpark.repository.StaffAccountRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer {

    private final ParkingSlotRepository slotRepo;
    private final StaffAccountRepository staffRepo;
    private final BookingRepository bookingRepo;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataInitializer(ParkingSlotRepository slotRepo,
                           StaffAccountRepository staffRepo,
                           BookingRepository bookingRepo,
                           BCryptPasswordEncoder passwordEncoder) {
        this.slotRepo        = slotRepo;
        this.staffRepo       = staffRepo;
        this.bookingRepo     = bookingRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void initializeData() {
        // Khởi tạo parking slots - BATCH INSERT
        if (slotRepo.count() == 0) {
            java.util.List<ParkingSlot> slots = new java.util.ArrayList<>();
            
            String[] motoRows = {"A","B","C","D","E"};
            for (String row : motoRows)
                for (int col = 1; col <= 10; col++)
                    slots.add(new ParkingSlot("M-" + row + col, "motorbike"));

            String[] carRows = {"A","B","C"};
            for (String row : carRows)
                for (int col = 1; col <= 6; col++)
                    slots.add(new ParkingSlot("C-" + row + col, "car"));
            
            slotRepo.saveAll(slots); // Batch insert thay vì save từng cái
        }

        // Khởi tạo staff accounts - BATCH INSERT
        if (staffRepo.count() == 0) {
            java.util.List<StaffAccount> accounts = new java.util.ArrayList<>();
            
            StaffAccount admin = new StaffAccount("AD001", "Admin Hệ Thống",  "admin",   "huyhgbv1204@gmail.com",           passwordEncoder.encode("admin123"), "admin");
            admin.setVerified(true);
            admin.setActive(true);
            accounts.add(admin);
            
            StaffAccount baove1 = new StaffAccount("BV001", "Nguyễn Văn An",   "baove1",  "huynguyenthanh12406@gmail.com",   passwordEncoder.encode("baove123"), "staff");
            baove1.setVerified(true);
            baove1.setActive(true);
            accounts.add(baove1);
            
            StaffAccount baove2 = new StaffAccount("BV002", "Trần Thị Bình",   "baove2",  "huyth1204@gmail.com",             passwordEncoder.encode("baove123"), "staff");
            baove2.setVerified(true);
            baove2.setActive(true);
            accounts.add(baove2);
            
            staffRepo.saveAll(accounts); // Batch insert
        }

        // Khởi tạo demo data: xe đang đỗ và lịch sử giao dịch - BATCH INSERT
        if (bookingRepo.count() == 0) {
            LocalDateTime now = LocalDateTime.now();
            java.util.List<ParkingSlot> slotsToUpdate = new java.util.ArrayList<>();
            java.util.List<Booking> bookings = new java.util.ArrayList<>();
            
            // 1. Xe đang đỗ (15 xe)
            addParkedVehicle(slotsToUpdate, bookings, "M-A1", "Nguyễn Văn A", "29A-12345", "xe_may", now.minusHours(3));
            addParkedVehicle(slotsToUpdate, bookings, "M-A5", "Trần Thị B", "30B-67890", "xe_may", now.minusHours(8));
            addParkedVehicle(slotsToUpdate, bookings, "M-B2", "Lê Văn C", "51C-11111", "xe_may", now.minusHours(15));
            addParkedVehicle(slotsToUpdate, bookings, "M-B7", "Phạm Thị D", "29D-22222", "xe_may", now.minusHours(20));
            addParkedVehicle(slotsToUpdate, bookings, "M-C3", "Hoàng Văn E", "30E-33333", "xe_may", now.minusHours(5));
            addParkedVehicle(slotsToUpdate, bookings, "M-C8", "Võ Thị F", "51F-44444", "xe_may", now.minusHours(10));
            addParkedVehicle(slotsToUpdate, bookings, "M-D1", "Đặng Văn G", "29G-55555", "xe_may", now.minusHours(2));
            addParkedVehicle(slotsToUpdate, bookings, "M-D9", "Bùi Thị H", "30H-66666", "xe_may", now.minusHours(18));
            addParkedVehicle(slotsToUpdate, bookings, "M-E4", "Phan Văn I", "51I-77777", "xe_may", now.minusHours(7));
            addParkedVehicle(slotsToUpdate, bookings, "M-E6", "Dương Thị K", "29K-88888", "xe_may", now.minusHours(12));
            addParkedVehicle(slotsToUpdate, bookings, "C-A1", "Nguyễn Minh L", "29L-99999", "o_to", now.minusHours(4));
            addParkedVehicle(slotsToUpdate, bookings, "C-A3", "Trần Văn M", "30M-10101", "o_to", now.minusHours(9));
            addParkedVehicle(slotsToUpdate, bookings, "C-B2", "Lê Thị N", "51N-20202", "o_to", now.minusHours(14));
            addParkedVehicle(slotsToUpdate, bookings, "C-B5", "Phạm Văn O", "29O-30303", "o_to", now.minusHours(6));
            addParkedVehicle(slotsToUpdate, bookings, "C-C1", "Hoàng Thị P", "30P-40404", "o_to", now.minusHours(11));
            
            // 2. Lịch sử giao dịch đã hoàn thành (28 giao dịch)
            addCompletedBooking(bookings, "Nguyễn Văn Q", "29Q-50505", "xe_may", now.minusDays(1).minusHours(10), now.minusDays(1).minusHours(2), 15000L);
            addCompletedBooking(bookings, "Trần Thị R", "30R-60606", "xe_may", now.minusDays(1).minusHours(15), now.minusDays(1).minusHours(1), 30000L);
            addCompletedBooking(bookings, "Lê Văn S", "51S-70707", "xe_may", now.minusDays(2).minusHours(5), now.minusDays(2).minusHours(1), 15000L);
            addCompletedBooking(bookings, "Phạm Thị T", "29T-80808", "xe_may", now.minusDays(2).minusHours(20), now.minusDays(2).minusHours(4), 30000L);
            addCompletedBooking(bookings, "Hoàng Văn U", "30U-90909", "xe_may", now.minusDays(3).minusHours(30), now.minusDays(3).minusHours(4), 45000L);
            addCompletedBooking(bookings, "Võ Thị V", "51V-11122", "xe_may", now.minusDays(3).minusHours(11), now.minusDays(3).minusHours(2), 15000L);
            addCompletedBooking(bookings, "Đặng Văn W", "29W-22233", "xe_may", now.minusDays(4).minusHours(6), now.minusDays(4).minusHours(1), 15000L);
            addCompletedBooking(bookings, "Bùi Thị X", "30X-33344", "xe_may", now.minusDays(4).minusHours(25), now.minusDays(4).minusHours(1), 45000L);
            addCompletedBooking(bookings, "Phan Văn Y", "51Y-44455", "xe_may", now.minusDays(5).minusHours(13), now.minusDays(5).minusHours(2), 30000L);
            addCompletedBooking(bookings, "Dương Thị Z", "29Z-55566", "xe_may", now.minusDays(5).minusHours(8), now.minusDays(5).minusHours(3), 15000L);
            addCompletedBooking(bookings, "Nguyễn Văn AA", "30A-66677", "xe_may", now.minusDays(6).minusHours(19), now.minusDays(6).minusHours(5), 30000L);
            addCompletedBooking(bookings, "Trần Thị BB", "51B-77788", "xe_may", now.minusDays(6).minusHours(10), now.minusDays(6).minusHours(4), 15000L);
            addCompletedBooking(bookings, "Lê Văn CC", "29C-88899", "xe_may", now.minusDays(7).minusHours(7), now.minusDays(7).minusHours(2), 15000L);
            addCompletedBooking(bookings, "Phạm Thị DD", "30D-99900", "xe_may", now.minusDays(7).minusHours(15), now.minusDays(7).minusHours(3), 30000L);
            addCompletedBooking(bookings, "Hoàng Văn EE", "51E-10011", "o_to", now.minusDays(1).minusHours(9), now.minusDays(1).minusHours(2), 30000L);
            addCompletedBooking(bookings, "Võ Thị FF", "29F-20022", "o_to", now.minusDays(1).minusHours(16), now.minusDays(1).minusHours(3), 60000L);
            addCompletedBooking(bookings, "Đặng Văn GG", "30G-30033", "o_to", now.minusDays(2).minusHours(11), now.minusDays(2).minusHours(1), 30000L);
            addCompletedBooking(bookings, "Bùi Thị HH", "51H-40044", "o_to", now.minusDays(2).minusHours(28), now.minusDays(2).minusHours(3), 90000L);
            addCompletedBooking(bookings, "Phan Văn II", "29I-50055", "o_to", now.minusDays(3).minusHours(14), now.minusDays(3).minusHours(2), 60000L);
            addCompletedBooking(bookings, "Dương Thị JJ", "30J-60066", "o_to", now.minusDays(3).minusHours(8), now.minusDays(3).minusHours(3), 30000L);
            addCompletedBooking(bookings, "Nguyễn Văn KK", "51K-70077", "o_to", now.minusDays(4).minusHours(20), now.minusDays(4).minusHours(4), 60000L);
            addCompletedBooking(bookings, "Trần Thị LL", "29L-80088", "o_to", now.minusDays(4).minusHours(10), now.minusDays(4).minusHours(2), 30000L);
            addCompletedBooking(bookings, "Lê Văn MM", "30M-90099", "o_to", now.minusDays(5).minusHours(26), now.minusDays(5).minusHours(1), 90000L);
            addCompletedBooking(bookings, "Phạm Thị NN", "51N-10100", "o_to", now.minusDays(5).minusHours(13), now.minusDays(5).minusHours(5), 30000L);
            addCompletedBooking(bookings, "Hoàng Văn OO", "29O-20200", "o_to", now.minusDays(6).minusHours(15), now.minusDays(6).minusHours(3), 60000L);
            addCompletedBooking(bookings, "Võ Thị PP", "30P-30300", "o_to", now.minusDays(6).minusHours(9), now.minusDays(6).minusHours(4), 30000L);
            addCompletedBooking(bookings, "Đặng Văn QQ", "51Q-40400", "o_to", now.minusDays(7).minusHours(18), now.minusDays(7).minusHours(6), 60000L);
            addCompletedBooking(bookings, "Bùi Thị RR", "29R-50500", "o_to", now.minusDays(7).minusHours(11), now.minusDays(7).minusHours(3), 30000L);
            
            // Batch insert tất cả
            slotRepo.saveAll(slotsToUpdate);
            bookingRepo.saveAll(bookings);
        }
    }
    
    private void addParkedVehicle(java.util.List<ParkingSlot> slotsToUpdate, java.util.List<Booking> bookings, 
                                  String slotId, String customerName, String licensePlate, String vehicleType, LocalDateTime checkIn) {
        // Cập nhật parking slot
        ParkingSlot slot = slotRepo.findById(slotId).orElse(null);
        if (slot != null) {
            slot.setOccupied(true);
            slot.setLicensePlate(licensePlate);
            slot.setCheckinTime(checkIn.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            slotsToUpdate.add(slot);
        }
        
        // Tạo booking đang pending
        Booking booking = new Booking();
        booking.setCustomerName(customerName);
        booking.setLicensePlate(licensePlate);
        booking.setVehicleType(vehicleType);
        booking.setCheckIn(checkIn);
        booking.setStatus("PENDING");
        booking.setPaymentCode(generatePaymentCode());
        bookings.add(booking);
    }
    
    private void addCompletedBooking(java.util.List<Booking> bookings, String customerName, String licensePlate, String vehicleType, 
                                    LocalDateTime checkIn, LocalDateTime checkOut, Long amount) {
        Booking booking = new Booking();
        booking.setCustomerName(customerName);
        booking.setLicensePlate(licensePlate);
        booking.setVehicleType(vehicleType);
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        booking.setStatus("PAID");
        booking.setAmountDue(amount);
        booking.setPaidAt(checkOut);
        booking.setPaymentCode(generatePaymentCode());
        bookings.add(booking);
    }
    
    /**
     * Generate payment code (format: SP + 6 random characters)
     */
    private String generatePaymentCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("SP");
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
