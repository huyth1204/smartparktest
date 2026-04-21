package com.smartpark;

import com.smartpark.model.Booking;
import com.smartpark.model.ParkingSlot;
import com.smartpark.model.StaffAccount;
import com.smartpark.repository.BookingRepository;
import com.smartpark.repository.ParkingSlotRepository;
import com.smartpark.repository.StaffAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

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

    @Override
    public void run(String... args) {
        // Khởi tạo parking slots
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

        // Khởi tạo staff accounts
        if (staffRepo.count() == 0) {
            StaffAccount admin = new StaffAccount("AD001", "Admin Hệ Thống",  "admin",   "huyhgbv1204@gmail.com",           passwordEncoder.encode("admin123"), "admin");
            admin.setVerified(true); // Tài khoản demo đã xác nhận
            staffRepo.save(admin);
            
            StaffAccount baove1 = new StaffAccount("BV001", "Nguyễn Văn An",   "baove1",  "huynguyenthanh12406@gmail.com",   passwordEncoder.encode("baove123"), "staff");
            baove1.setVerified(true);
            staffRepo.save(baove1);
            
            StaffAccount baove2 = new StaffAccount("BV002", "Trần Thị Bình",   "baove2",  "huyth1204@gmail.com",             passwordEncoder.encode("baove123"), "staff");
            baove2.setVerified(true);
            staffRepo.save(baove2);
        }

        // Khởi tạo demo data: xe đang đỗ và lịch sử giao dịch
        if (bookingRepo.count() == 0) {
            LocalDateTime now = LocalDateTime.now();
            
            // 1. Xe đang đỗ (10-15 xe)
            createParkedVehicle("M-A1", "Nguyễn Văn A", "29A-12345", "xe_may", now.minusHours(3));
            createParkedVehicle("M-A5", "Trần Thị B", "30B-67890", "xe_may", now.minusHours(8));
            createParkedVehicle("M-B2", "Lê Văn C", "51C-11111", "xe_may", now.minusHours(15));
            createParkedVehicle("M-B7", "Phạm Thị D", "29D-22222", "xe_may", now.minusHours(20));
            createParkedVehicle("M-C3", "Hoàng Văn E", "30E-33333", "xe_may", now.minusHours(5));
            createParkedVehicle("M-C8", "Võ Thị F", "51F-44444", "xe_may", now.minusHours(10));
            createParkedVehicle("M-D1", "Đặng Văn G", "29G-55555", "xe_may", now.minusHours(2));
            createParkedVehicle("M-D9", "Bùi Thị H", "30H-66666", "xe_may", now.minusHours(18));
            createParkedVehicle("M-E4", "Phan Văn I", "51I-77777", "xe_may", now.minusHours(7));
            createParkedVehicle("M-E6", "Dương Thị K", "29K-88888", "xe_may", now.minusHours(12));
            
            createParkedVehicle("C-A1", "Nguyễn Minh L", "29L-99999", "o_to", now.minusHours(4));
            createParkedVehicle("C-A3", "Trần Văn M", "30M-10101", "o_to", now.minusHours(9));
            createParkedVehicle("C-B2", "Lê Thị N", "51N-20202", "o_to", now.minusHours(14));
            createParkedVehicle("C-B5", "Phạm Văn O", "29O-30303", "o_to", now.minusHours(6));
            createParkedVehicle("C-C1", "Hoàng Thị P", "30P-40404", "o_to", now.minusHours(11));
            
            // 2. Lịch sử giao dịch đã hoàn thành (20-30 giao dịch)
            // Xe máy - các khoảng thời gian khác nhau
            createCompletedBooking("Nguyễn Văn Q", "29Q-50505", "xe_may", now.minusDays(1).minusHours(10), now.minusDays(1).minusHours(2), 15000L);  // 8h = 1 slot
            createCompletedBooking("Trần Thị R", "30R-60606", "xe_may", now.minusDays(1).minusHours(15), now.minusDays(1).minusHours(1), 30000L);   // 14h = 2 slots
            createCompletedBooking("Lê Văn S", "51S-70707", "xe_may", now.minusDays(2).minusHours(5), now.minusDays(2).minusHours(1), 15000L);      // 4h = 1 slot
            createCompletedBooking("Phạm Thị T", "29T-80808", "xe_may", now.minusDays(2).minusHours(20), now.minusDays(2).minusHours(4), 30000L);   // 16h = 2 slots
            createCompletedBooking("Hoàng Văn U", "30U-90909", "xe_may", now.minusDays(3).minusHours(30), now.minusDays(3).minusHours(4), 45000L);  // 26h = 3 slots
            createCompletedBooking("Võ Thị V", "51V-11122", "xe_may", now.minusDays(3).minusHours(11), now.minusDays(3).minusHours(2), 15000L);     // 9h = 1 slot
            createCompletedBooking("Đặng Văn W", "29W-22233", "xe_may", now.minusDays(4).minusHours(6), now.minusDays(4).minusHours(1), 15000L);    // 5h = 1 slot
            createCompletedBooking("Bùi Thị X", "30X-33344", "xe_may", now.minusDays(4).minusHours(25), now.minusDays(4).minusHours(1), 45000L);    // 24h = 2 slots
            createCompletedBooking("Phan Văn Y", "51Y-44455", "xe_may", now.minusDays(5).minusHours(13), now.minusDays(5).minusHours(2), 30000L);   // 11h = 1 slot
            createCompletedBooking("Dương Thị Z", "29Z-55566", "xe_may", now.minusDays(5).minusHours(8), now.minusDays(5).minusHours(3), 15000L);   // 5h = 1 slot
            createCompletedBooking("Nguyễn Văn AA", "30A-66677", "xe_may", now.minusDays(6).minusHours(19), now.minusDays(6).minusHours(5), 30000L); // 14h = 2 slots
            createCompletedBooking("Trần Thị BB", "51B-77788", "xe_may", now.minusDays(6).minusHours(10), now.minusDays(6).minusHours(4), 15000L);  // 6h = 1 slot
            createCompletedBooking("Lê Văn CC", "29C-88899", "xe_may", now.minusDays(7).minusHours(7), now.minusDays(7).minusHours(2), 15000L);     // 5h = 1 slot
            createCompletedBooking("Phạm Thị DD", "30D-99900", "xe_may", now.minusDays(7).minusHours(15), now.minusDays(7).minusHours(3), 30000L);  // 12h = 1 slot
            
            // Ô tô - các khoảng thời gian khác nhau
            createCompletedBooking("Hoàng Văn EE", "51E-10011", "o_to", now.minusDays(1).minusHours(9), now.minusDays(1).minusHours(2), 30000L);    // 7h = 1 slot
            createCompletedBooking("Võ Thị FF", "29F-20022", "o_to", now.minusDays(1).minusHours(16), now.minusDays(1).minusHours(3), 60000L);      // 13h = 2 slots
            createCompletedBooking("Đặng Văn GG", "30G-30033", "o_to", now.minusDays(2).minusHours(11), now.minusDays(2).minusHours(1), 30000L);    // 10h = 1 slot
            createCompletedBooking("Bùi Thị HH", "51H-40044", "o_to", now.minusDays(2).minusHours(28), now.minusDays(2).minusHours(3), 90000L);     // 25h = 3 slots
            createCompletedBooking("Phan Văn II", "29I-50055", "o_to", now.minusDays(3).minusHours(14), now.minusDays(3).minusHours(2), 60000L);    // 12h = 1 slot
            createCompletedBooking("Dương Thị JJ", "30J-60066", "o_to", now.minusDays(3).minusHours(8), now.minusDays(3).minusHours(3), 30000L);    // 5h = 1 slot
            createCompletedBooking("Nguyễn Văn KK", "51K-70077", "o_to", now.minusDays(4).minusHours(20), now.minusDays(4).minusHours(4), 60000L);  // 16h = 2 slots
            createCompletedBooking("Trần Thị LL", "29L-80088", "o_to", now.minusDays(4).minusHours(10), now.minusDays(4).minusHours(2), 30000L);    // 8h = 1 slot
            createCompletedBooking("Lê Văn MM", "30M-90099", "o_to", now.minusDays(5).minusHours(26), now.minusDays(5).minusHours(1), 90000L);      // 25h = 3 slots
            createCompletedBooking("Phạm Thị NN", "51N-10100", "o_to", now.minusDays(5).minusHours(13), now.minusDays(5).minusHours(5), 30000L);    // 8h = 1 slot
            createCompletedBooking("Hoàng Văn OO", "29O-20200", "o_to", now.minusDays(6).minusHours(15), now.minusDays(6).minusHours(3), 60000L);   // 12h = 1 slot
            createCompletedBooking("Võ Thị PP", "30P-30300", "o_to", now.minusDays(6).minusHours(9), now.minusDays(6).minusHours(4), 30000L);       // 5h = 1 slot
            createCompletedBooking("Đặng Văn QQ", "51Q-40400", "o_to", now.minusDays(7).minusHours(18), now.minusDays(7).minusHours(6), 60000L);    // 12h = 1 slot
            createCompletedBooking("Bùi Thị RR", "29R-50500", "o_to", now.minusDays(7).minusHours(11), now.minusDays(7).minusHours(3), 30000L);     // 8h = 1 slot
        }
    }
    
    private void createParkedVehicle(String slotId, String customerName, String licensePlate, String vehicleType, LocalDateTime checkIn) {
        // Cập nhật parking slot
        ParkingSlot slot = slotRepo.findById(slotId).orElse(null);
        if (slot != null) {
            slot.setOccupied(true);
            slot.setLicensePlate(licensePlate);
            // Format as HH:mm to match ParkingSlotService.checkin()
            slot.setCheckinTime(checkIn.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            slotRepo.save(slot);
        }
        
        // Tạo booking đang pending
        Booking booking = new Booking();
        booking.setCustomerName(customerName);
        booking.setLicensePlate(licensePlate);
        booking.setVehicleType(vehicleType);
        booking.setCheckIn(checkIn);
        booking.setStatus("PENDING");
        booking.setPaymentCode(generatePaymentCode()); // FIX: Generate paymentCode
        bookingRepo.save(booking);
    }
    
    private void createCompletedBooking(String customerName, String licensePlate, String vehicleType, 
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
        booking.setPaymentCode(generatePaymentCode()); // FIX: Generate paymentCode
        bookingRepo.save(booking);
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
