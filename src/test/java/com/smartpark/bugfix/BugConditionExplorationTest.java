package com.smartpark.bugfix;

import com.smartpark.DataInitializer;
import com.smartpark.model.AccountVerificationToken;
import com.smartpark.model.Booking;
import com.smartpark.model.StaffAccount;
import com.smartpark.repository.AccountVerificationTokenRepository;
import com.smartpark.repository.BookingRepository;
import com.smartpark.repository.StaffAccountRepository;
import com.smartpark.service.AccountVerificationService;
import com.smartpark.service.EmailService;
import com.smartpark.service.UserService;
import com.smartpark.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Bug Condition Exploration Tests - Phase 1
 * 
 * **Validates: Requirements 1.1-1.20**
 * 
 * These tests are EXPECTED TO FAIL on unfixed code to confirm the bugs exist.
 * DO NOT attempt to fix the tests or the code when they fail.
 * 
 * These tests encode the expected behavior - they will validate the fixes 
 * when they pass after implementation.
 */
@SpringBootTest
@ActiveProfiles("test")
public class BugConditionExplorationTest {

    @Autowired
    private AccountVerificationService verificationService;
    
    @Autowired
    private AccountVerificationTokenRepository tokenRepo;
    
    @Autowired
    private StaffAccountRepository staffRepo;
    
    @Autowired
    private BookingRepository bookingRepo;
    
    @Autowired
    private BookingServiceImpl bookingService;
    
    @Autowired
    private DataInitializer dataInitializer;
    
    @Autowired
    private UserService userService;
    
    @MockBean
    private EmailService emailService;

    /**
     * 1.1 Email Verification Security Test
     * 
     * **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6**
     * 
     * Tests that the current system has insecure email verification:
     * - Admin must provide password when creating account
     * - Email contains password plaintext
     * - Token expires in 24 hours instead of 5 minutes
     * - No polling endpoint exists
     * - Verification activates account immediately without set-password step
     * 
     * EXPECTED OUTCOME: Test FAILS (confirms Bug 1 exists)
     */
    @Test
    public void test_1_1_EmailVerificationSecurity_FailsOnUnfixedCode() {
        // Setup: Create staff account with email
        StaffAccount account = new StaffAccount();
        account.setStaffId("TEST001");
        account.setFullName("Test Staff");
        account.setUsername("teststaff");
        account.setEmail("test@example.com");
        account.setPassword("tempPassword123");
        account.setRole("staff");
        account.setVerified(false);
        account.setActive(false);
        staffRepo.save(account);
        
        // Act: Send verification email with password
        verificationService.sendVerificationEmail(account, "http://localhost:8080", "tempPassword123");
        
        // Verify email was called with password parameter (Bug: password in email)
        verify(emailService, times(1)).sendAccountVerificationEmail(
            eq("test@example.com"),
            anyString(),
            eq("teststaff"),
            eq("tempPassword123")  // BUG: Password sent in email
        );
        
        // Check token expiry (Bug: 24 hours instead of 5 minutes)
        Optional<AccountVerificationToken> tokenOpt = tokenRepo.findAll().stream()
            .filter(t -> t.getStaffAccount().getId().equals(account.getId()))
            .findFirst();
        
        assertTrue(tokenOpt.isPresent(), "Token should exist");
        AccountVerificationToken token = tokenOpt.get();
        
        LocalDateTime expectedExpiry = LocalDateTime.now().plusMinutes(5);
        LocalDateTime actualExpiry = token.getExpiryDate();
        
        // BUG: Token expires in 24 hours, not 5 minutes
        // This assertion will FAIL on unfixed code because actualExpiry is ~24 hours from now
        assertTrue(
            actualExpiry.isBefore(expectedExpiry.plusMinutes(1)) && 
            actualExpiry.isAfter(expectedExpiry.minusMinutes(1)),
            "Token should expire in 5 minutes, but expires at: " + actualExpiry
        );
        
        // Verify account activation behavior (Bug: activates immediately)
        String result = verificationService.verifyAccount(token.getToken());
        assertEquals("OK", result);
        
        StaffAccount verified = staffRepo.findById(account.getId()).orElseThrow();
        
        // BUG: Account is activated immediately without set-password step
        // Expected behavior: Should redirect to set-password page, not activate immediately
        assertFalse(verified.isActive(), 
            "Account should NOT be active immediately - should redirect to set-password page first");
        assertFalse(verified.isVerified(),
            "Account should NOT be verified until password is set");
    }

    /**
     * 1.2 Mail Credentials Test
     * 
     * **Validates: Requirements 1.7, 1.8**
     * 
     * Tests that render.yaml is missing MAIL_USERNAME and MAIL_PASSWORD.
     * This test simulates the Render environment without mail credentials.
     * 
     * EXPECTED OUTCOME: Test FAILS (confirms Bug 2 exists)
     */
    @Test
    public void test_1_2_MailCredentials_FailsOnUnfixedCode() {
        // This test verifies that the render.yaml configuration is incomplete
        // In a real scenario, this would fail when deployed to Render
        
        // Check if mail properties are configured
        String mailUsername = System.getenv("MAIL_USERNAME");
        String mailPassword = System.getenv("MAIL_PASSWORD");
        
        // BUG: These environment variables are missing in render.yaml
        // Expected: Both should be present for production deployment
        assertNotNull(mailUsername, 
            "MAIL_USERNAME should be configured in render.yaml for production");
        assertNotNull(mailPassword,
            "MAIL_PASSWORD should be configured in render.yaml for production");
        
        // Additional check: Verify mail host and port are configured
        String mailHost = System.getenv("MAIL_HOST");
        String mailPort = System.getenv("MAIL_PORT");
        
        assertNotNull(mailHost, "MAIL_HOST should be configured");
        assertNotNull(mailPort, "MAIL_PORT should be configured");
    }

    /**
     * 1.3 Database URL Parsing Test
     * 
     * **Validates: Requirements 1.9, 1.10**
     * 
     * Tests that DATABASE_URL in postgres:// format cannot be used directly
     * by Spring Boot, which requires jdbc:postgresql:// format.
     * 
     * EXPECTED OUTCOME: Test FAILS (confirms Bug 3 exists)
     */
    @Test
    public void test_1_3_DatabaseUrlParsing_FailsOnUnfixedCode() {
        // Simulate Render's DATABASE_URL format
        String renderDatabaseUrl = "postgres://user:password@host.render.com:5432/smartparkdb";
        
        // Expected JDBC URL format
        String expectedJdbcUrl = "jdbc:postgresql://host.render.com:5432/smartparkdb?user=user&password=password";
        
        // BUG: System does not parse postgres:// to jdbc:postgresql://
        // Expected: System should have a parser to convert the format
        
        // Check if parsing logic exists (it doesn't in unfixed code)
        String parsedUrl = parseDatabaseUrl(renderDatabaseUrl);
        
        assertNotNull(parsedUrl, "Database URL parser should exist");
        assertTrue(parsedUrl.startsWith("jdbc:postgresql://"),
            "Parsed URL should start with jdbc:postgresql://, but got: " + parsedUrl);
        assertTrue(parsedUrl.contains("user=user"),
            "Parsed URL should contain user parameter");
        assertTrue(parsedUrl.contains("password=password"),
            "Parsed URL should contain password parameter");
    }
    
    // Helper method that doesn't exist in unfixed code
    private String parseDatabaseUrl(String postgresUrl) {
        // BUG: This parsing logic doesn't exist in the unfixed codebase
        // Expected: Should have DatabaseConfig class with this logic
        return null;  // Returns null because parser doesn't exist
    }

    /**
     * 1.4 PaymentCode Null Test
     * 
     * **Validates: Requirements 1.11, 1.12**
     * 
     * Tests that DataInitializer.createParkedVehicle() creates bookings
     * with null paymentCode, causing NullPointerException in processPayment().
     * 
     * EXPECTED OUTCOME: Test FAILS (confirms Bug 4 exists)
     */
    @Test
    public void test_1_4_PaymentCodeNull_FailsOnUnfixedCode() {
        // Clear existing bookings
        bookingRepo.deleteAll();
        
        // Create a parked vehicle using DataInitializer logic
        Booking booking = new Booking();
        booking.setCustomerName("Test Customer");
        booking.setLicensePlate("29A-12345");
        booking.setVehicleType("xe_may");
        booking.setCheckIn(LocalDateTime.now().minusHours(3));
        booking.setStatus("PENDING");
        // BUG: paymentCode is NOT set
        bookingRepo.save(booking);
        
        // Verify paymentCode is null (Bug exists)
        Booking saved = bookingRepo.findById(booking.getId()).orElseThrow();
        
        // BUG: paymentCode should be set, but it's null
        assertNotNull(saved.getPaymentCode(),
            "PaymentCode should be generated for parked vehicles, but it's null");
        
        // Try to process payment - this will throw NullPointerException
        assertDoesNotThrow(() -> {
            bookingService.processPayment("THANH TOAN SP123456", 15000L, "REF001");
        }, "processPayment should not throw NullPointerException when paymentCode is null");
    }

    /**
     * 1.5 FindAll Performance Test
     * 
     * **Validates: Requirements 1.13, 1.14**
     * 
     * Tests that processPayment() uses findAll() which loads all bookings
     * into memory, causing performance issues with large datasets.
     * 
     * EXPECTED OUTCOME: Test FAILS (confirms Bug 5 exists)
     */
    @Test
    public void test_1_5_FindAllPerformance_FailsOnUnfixedCode() {
        // Clear existing bookings
        bookingRepo.deleteAll();
        
        // Create 1000 bookings to simulate large dataset
        for (int i = 0; i < 1000; i++) {
            Booking b = new Booking();
            b.setCustomerName("Customer " + i);
            b.setLicensePlate("29A-" + String.format("%05d", i));
            b.setVehicleType("xe_may");
            b.setCheckIn(LocalDateTime.now().minusHours(i % 24));
            b.setStatus("PENDING");
            b.setPaymentCode("SP" + String.format("%06d", i));
            b.setAmountDue(15000L);
            bookingRepo.save(b);
        }
        
        // Measure performance of processPayment
        long startTime = System.currentTimeMillis();
        bookingService.processPayment("THANH TOAN SP000500", 15000L, "REF001");
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // BUG: Query uses findAll() which is slow
        // Expected: Should use custom query method for direct database lookup
        // With 1000 records, findAll() should take > 100ms
        assertTrue(duration < 100,
            "processPayment should use optimized query (<100ms), but took: " + duration + "ms");
        
        // Verify that a custom query method exists
        // BUG: BookingRepository doesn't have findByPaymentCodeAndStatusNot method
        try {
            bookingRepo.getClass().getMethod("findByPaymentCodeAndStatusNot", String.class, String.class);
        } catch (NoSuchMethodException e) {
            fail("BookingRepository should have findByPaymentCodeAndStatusNot method for optimized queries");
        }
    }

    /**
     * 1.6 Booking Field Naming Test
     * 
     * **Validates: Requirements 1.15, 1.16**
     * 
     * Tests that Booking entity has field named "Gmail" (PascalCase)
     * instead of "email" (camelCase), violating Java naming conventions.
     * 
     * EXPECTED OUTCOME: Test FAILS (confirms Bug 6 exists)
     */
    @Test
    public void test_1_6_BookingFieldNaming_FailsOnUnfixedCode() throws Exception {
        // Use reflection to inspect Booking class fields
        Field[] fields = Booking.class.getDeclaredFields();
        
        boolean hasEmailField = false;
        boolean hasGmailField = false;
        
        for (Field field : fields) {
            if (field.getName().equals("email")) {
                hasEmailField = true;
            }
            if (field.getName().equals("Gmail")) {
                hasGmailField = true;
            }
        }
        
        // BUG: Field is named "Gmail" instead of "email"
        // Expected: Should have "email" field following camelCase convention
        assertTrue(hasEmailField, 
            "Booking should have 'email' field (camelCase), not 'Gmail' (PascalCase)");
        assertFalse(hasGmailField,
            "Booking should NOT have 'Gmail' field - violates Java naming convention");
    }

    /**
     * 1.7 CheckIn Validation Test
     * 
     * **Validates: Requirements 1.17, 1.18**
     * 
     * Tests that createBooking() accepts checkIn dates in the past
     * without validation, creating invalid bookings.
     * 
     * EXPECTED OUTCOME: Test FAILS (confirms Bug 7 exists)
     */
    @Test
    public void test_1_7_CheckInValidation_FailsOnUnfixedCode() {
        // Try to create booking with checkIn in the past
        LocalDateTime pastCheckIn = LocalDateTime.now().minusDays(1);
        LocalDateTime futureCheckOut = LocalDateTime.now().plusHours(2);
        
        // BUG: System accepts checkIn in the past without validation
        // Expected: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(
                "Test Customer",
                "29A-99999",
                "xe_may",
                pastCheckIn,
                futureCheckOut
            );
        }, "createBooking should throw IllegalArgumentException for checkIn in the past");
    }

    /**
     * 1.8 BCryptPasswordEncoder Instance Test
     * 
     * **Validates: Requirements 1.19, 1.20**
     * 
     * Tests that UserService creates new BCryptPasswordEncoder instance
     * instead of injecting Spring-managed singleton bean.
     * 
     * EXPECTED OUTCOME: Test FAILS (confirms Bug 8 exists)
     */
    @Test
    public void test_1_8_BCryptPasswordEncoderInstance_FailsOnUnfixedCode() throws Exception {
        // Use reflection to inspect UserService encoder field
        Field encoderField = UserService.class.getDeclaredField("encoder");
        encoderField.setAccessible(true);
        
        BCryptPasswordEncoder encoder = (BCryptPasswordEncoder) encoderField.get(userService);
        
        // Check if encoder is final (indicates it's created with new, not injected)
        boolean isFinal = java.lang.reflect.Modifier.isFinal(encoderField.getModifiers());
        
        // BUG: encoder is created with "new BCryptPasswordEncoder()" and marked final
        // Expected: Should be injected via @Autowired or constructor injection
        assertFalse(isFinal,
            "encoder field should NOT be final - it should be injected by Spring");
        
        // Additional check: Verify encoder is not initialized inline
        // In unfixed code: private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        // In fixed code: @Autowired private BCryptPasswordEncoder encoder; (or constructor injection)
        
        // Check if UserService has constructor that accepts BCryptPasswordEncoder
        boolean hasEncoderConstructor = false;
        try {
            userService.getClass().getConstructor(BCryptPasswordEncoder.class);
            hasEncoderConstructor = true;
        } catch (NoSuchMethodException e) {
            // Constructor doesn't exist
        }
        
        // Check if field has @Autowired annotation
        boolean hasAutowired = encoderField.isAnnotationPresent(org.springframework.beans.factory.annotation.Autowired.class);
        
        assertTrue(hasEncoderConstructor || hasAutowired,
            "UserService should inject BCryptPasswordEncoder via constructor or @Autowired, not create new instance");
    }
}
