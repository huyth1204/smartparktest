# Implementation Plan

## Phase 1: Exploration Tests (BEFORE Fix)

- [x] 1. Write bug condition exploration tests
  - **Property 1: Bug Condition** - Email Verification Security, Mail Credentials, Database URL, PaymentCode Null, FindAll Performance, Field Naming, CheckIn Validation, Encoder Injection
  - **CRITICAL**: These tests MUST FAIL on unfixed code - failures confirm the bugs exist
  - **DO NOT attempt to fix the tests or the code when they fail**
  - **NOTE**: These tests encode the expected behavior - they will validate the fixes when they pass after implementation
  - **GOAL**: Surface counterexamples that demonstrate the 8 bugs exist
  - **Scoped PBT Approach**: For deterministic bugs, scope properties to concrete failing cases to ensure reproducibility
  
  - [x] 1.1 Email Verification Security Test (will fail on unfixed code)
    - Test that admin can create account with email AND password input
    - Test that email contains password plaintext
    - Test that token expiry is 24 hours instead of 5 minutes
    - Test that no polling endpoint exists for verification status
    - Test that clicking verification link activates account immediately without set-password step
    - **EXPECTED OUTCOME**: Test FAILS (confirms Bug 1 exists)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_
  
  - [x] 1.2 Mail Credentials Test (will fail on unfixed code)
    - Mock Render environment without MAIL_USERNAME and MAIL_PASSWORD
    - Attempt to send email
    - Test that AuthenticationFailedException is thrown
    - **EXPECTED OUTCOME**: Test FAILS (confirms Bug 2 exists)
    - _Requirements: 1.7, 1.8_
  
  - [x] 1.3 Database URL Parsing Test (will fail on unfixed code)
    - Set DATABASE_URL = "postgres://user:pass@host:5432/db"
    - Attempt to connect database
    - Test that connection fails due to format incompatibility
    - **EXPECTED OUTCOME**: Test FAILS (confirms Bug 3 exists)
    - _Requirements: 1.9, 1.10_
  
  - [x] 1.4 PaymentCode Null Test (will fail on unfixed code)
    - Call DataInitializer.createParkedVehicle()
    - Verify booking.paymentCode is null
    - Call processPayment() with any payment code
    - Test that NullPointerException is thrown at b.getPaymentCode()
    - **EXPECTED OUTCOME**: Test FAILS (confirms Bug 4 exists)
    - _Requirements: 1.11, 1.12_
  
  - [x] 1.5 FindAll Performance Test (will fail on unfixed code)
    - Create 10,000 bookings in test database
    - Measure time and memory usage of processPayment()
    - Test that query time > 1s or memory usage > 100MB
    - **EXPECTED OUTCOME**: Test FAILS (confirms Bug 5 exists)
    - _Requirements: 1.13, 1.14_
  
  - [x] 1.6 Booking Field Naming Test (will fail on unfixed code)
    - Inspect Booking.class using reflection
    - Test that field name is "Gmail" (PascalCase) instead of "email" (camelCase)
    - **EXPECTED OUTCOME**: Test FAILS (confirms Bug 6 exists)
    - _Requirements: 1.15, 1.16_
  
  - [x] 1.7 CheckIn Validation Test (will fail on unfixed code)
    - Call createBooking() with checkIn date in the past
    - Test that booking is created successfully (no exception thrown)
    - **EXPECTED OUTCOME**: Test FAILS (confirms Bug 7 exists)
    - _Requirements: 1.17, 1.18_
  
  - [x] 1.8 BCryptPasswordEncoder Instance Test (will fail on unfixed code)
    - Inspect UserService.class using reflection or debug
    - Test that encoder is initialized with "new BCryptPasswordEncoder()" instead of injected
    - **EXPECTED OUTCOME**: Test FAILS (confirms Bug 8 exists)
    - _Requirements: 1.19, 1.20_

## Phase 2: Preservation Tests (BEFORE Fix)

- [ ] 2. Write preservation property tests (BEFORE implementing fixes)
  - **Property 2: Preservation** - Non-Buggy Behaviors Must Remain Unchanged
  - **IMPORTANT**: Follow observation-first methodology
  - Observe behavior on UNFIXED code for non-buggy inputs
  - Write property-based tests capturing observed behavior patterns from Preservation Requirements
  - Property-based testing generates many test cases for stronger guarantees
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (confirms baseline behavior to preserve)
  
  - [ ] 2.1 Account Creation Without Email Preservation
    - Observe: Creating account without email activates immediately on unfixed code
    - Write property-based test: for all account creation requests without email, account is active immediately
    - Verify test passes on UNFIXED code
    - _Requirements: 3.1_
  
  - [ ] 2.2 Existing Authentication Flows Preservation
    - Observe: Login, register, password reset work normally on unfixed code
    - Write property-based test: for all login/register/reset operations not involving bugs, behavior is unchanged
    - Verify test passes on UNFIXED code
    - _Requirements: 3.2, 3.3, 3.4, 3.5, 3.15, 3.16, 3.17_
  
  - [ ] 2.3 Booking and Payment Operations Preservation
    - Observe: Booking creation with valid checkIn, payment processing with valid paymentCode work on unfixed code
    - Write property-based test: for all valid booking/payment operations, behavior is unchanged
    - Verify test passes on UNFIXED code
    - _Requirements: 3.8, 3.9, 3.10, 3.13, 3.14_
  
  - [ ] 2.4 Database and Configuration Preservation
    - Observe: Local development with H2 database works on unfixed code
    - Write property-based test: for all local environments, H2 database is used
    - Verify test passes on UNFIXED code
    - _Requirements: 3.6, 3.7_
  
  - [ ] 2.5 Entity Fields and Lifecycle Preservation
    - Observe: All Booking fields except "Gmail" work correctly on unfixed code
    - Write property-based test: for all Booking persistence operations, other fields are unchanged
    - Verify test passes on UNFIXED code
    - _Requirements: 3.11, 3.12_

## Phase 3: Implementation

- [x] 3. Fix for 8 critical bugs

  - [x] 3.1 Fix Bug 1: Implement secure email verification flow
    - Remove password parameter from DashboardController.addAccount()
    - Update AccountVerificationService.sendVerificationEmail() to remove tempPassword parameter
    - Change token expiry from plusHours(24) to plusMinutes(5)
    - Update AccountVerificationService.verifyAccount() to redirect to set-password page instead of activating immediately
    - Add AccountVerificationService.setPassword() method to handle password setting
    - Add @Scheduled cleanup job to delete expired tokens and unverified accounts
    - Update EmailService.sendAccountVerificationEmail() to remove password from email content
    - Create DashboardController endpoints: GET /set-password and POST /set-password
    - Create set-password.html template with password form
    - Update admin-dashboard.html: remove password input, add polling script, add modal
    - Add polling endpoint GET /admin/accounts/verify-status/{id}
    - _Bug_Condition: isBugCondition(input) where input.operation == "CREATE_ACCOUNT_WITH_EMAIL" AND input.passwordInEmail == true_
    - _Expected_Behavior: Email does NOT contain password, token expires in 5 minutes, verification redirects to set-password page, polling API returns status_
    - _Preservation: Account creation without email still activates immediately (3.1)_
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_

  - [x] 3.2 Fix Bug 2: Add mail credentials to render.yaml
    - Add MAIL_USERNAME environment variable to render.yaml
    - Add MAIL_PASSWORD environment variable to render.yaml
    - Add MAIL_HOST environment variable to render.yaml
    - Add MAIL_PORT environment variable to render.yaml
    - Update application-prod.properties with spring.mail.* configuration
    - _Bug_Condition: isBugCondition(input) where input.operation == "DEPLOY_TO_RENDER" AND input.mailCredentialsMissing == true_
    - _Expected_Behavior: render.yaml includes MAIL_USERNAME and MAIL_PASSWORD, Spring Mail authenticates successfully_
    - _Preservation: Email reset password continues to work (3.4, 3.5)_
    - _Requirements: 2.8, 2.9_

  - [x] 3.3 Fix Bug 3: Parse DATABASE_URL to JDBC format
    - Create DatabaseConfig.java @Configuration class
    - Parse DATABASE_URL from environment (format: postgres://user:pass@host:port/db)
    - Extract user, password, host, port, database components
    - Build JDBC URL (format: jdbc:postgresql://host:port/db?user=xxx&password=xxx)
    - Configure DataSource bean with JDBC URL
    - Update application-prod.properties to use JDBC_DATABASE_URL if available
    - _Bug_Condition: isBugCondition(input) where input.operation == "CONNECT_DATABASE" AND input.databaseUrlFormat == "postgres://"_
    - _Expected_Behavior: System parses postgres:// to jdbc:postgresql:// format and connects successfully_
    - _Preservation: Local development with H2 continues to work (3.6, 3.7)_
    - _Requirements: 2.10, 2.11_

  - [x] 3.4 Fix Bug 4: Set paymentCode for parked vehicles
    - Extract generatePaymentCode() method to utility or copy to DataInitializer
    - In DataInitializer.createParkedVehicle(), generate paymentCode
    - Set booking.setPaymentCode(generatedCode) before bookingRepo.save()
    - _Bug_Condition: isBugCondition(input) where input.operation == "CREATE_PARKED_VEHICLE" AND input.paymentCode == null_
    - _Expected_Behavior: All parked vehicles have non-null paymentCode, processPayment() does not throw NullPointerException_
    - _Preservation: Bookings created via createBooking() continue to have paymentCode (3.8)_
    - _Requirements: 2.12, 2.13_

  - [x] 3.5 Fix Bug 5: Optimize processPayment query
    - Add custom query method to BookingRepository: findByPaymentCodeAndStatusNot(String paymentCode, String status)
    - Replace repo.findAll().stream().filter(...) in BookingServiceImpl.processPayment()
    - Use repo.findByPaymentCodeAndStatusNot(paymentCode, "PAID") for direct database query
    - _Bug_Condition: isBugCondition(input) where input.operation == "PROCESS_PAYMENT" AND input.usesFindAll == true_
    - _Expected_Behavior: processPayment() uses custom query with WHERE clause, query time < 100ms, memory usage < 10MB_
    - _Preservation: Payment processing for valid bookings continues to update status="PAID" (3.9)_
    - _Requirements: 2.14, 2.15_

  - [x] 3.6 Fix Bug 6: Rename Booking.Gmail to email
    - Rename field from "private String Gmail;" to "private String email;" in Booking.java
    - Lombok will auto-generate correct getter/setter
    - Create database migration script if needed: ALTER TABLE bookings RENAME COLUMN gmail TO email
    - _Bug_Condition: isBugCondition(input) where input.operation == "DEFINE_BOOKING_FIELD" AND input.fieldName == "Gmail"_
    - _Expected_Behavior: Field name is "email" (camelCase), follows Java naming convention_
    - _Preservation: All other Booking fields remain unchanged (3.11, 3.12)_
    - _Requirements: 2.16, 2.17_

  - [x] 3.7 Fix Bug 7: Add checkIn validation
    - Add validation at start of BookingServiceImpl.createBooking()
    - Check if (checkIn.isBefore(LocalDateTime.now())) throw new IllegalArgumentException("Ngày check-in không được trong quá khứ")
    - _Bug_Condition: isBugCondition(input) where input.operation == "CREATE_BOOKING" AND input.checkInInPast == true_
    - _Expected_Behavior: createBooking() throws IllegalArgumentException when checkIn is in the past_
    - _Preservation: Booking creation with valid checkIn continues to work (3.8, 3.13)_
    - _Requirements: 2.18, 2.19_

  - [x] 3.8 Fix Bug 8: Inject BCryptPasswordEncoder into UserService
    - Remove "private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();" from UserService
    - Add constructor injection: public UserService(BCryptPasswordEncoder encoder, ...) { this.encoder = encoder; }
    - Or use @Autowired field injection: @Autowired private BCryptPasswordEncoder encoder;
    - _Bug_Condition: isBugCondition(input) where input.operation == "INIT_USER_SERVICE" AND input.createsNewEncoder == true_
    - _Expected_Behavior: UserService injects BCryptPasswordEncoder bean, uses Spring-managed singleton_
    - _Preservation: UserService.register(), login(), resetPassword() continue to work (3.15, 3.16, 3.17)_
    - _Requirements: 2.20, 2.21_

  - [x] 3.9 Verify all bug condition exploration tests now pass
    - **Property 1: Expected Behavior** - All 8 Bugs Fixed
    - **IMPORTANT**: Re-run the SAME tests from task 1 - do NOT write new tests
    - The tests from task 1 encode the expected behavior
    - When these tests pass, it confirms the expected behaviors are satisfied
    - Run all 8 exploration tests from step 1
    - **EXPECTED OUTCOME**: All tests PASS (confirms all bugs are fixed)
    - _Requirements: 2.1-2.21 (all Expected Behavior Properties)_

  - [x] 3.10 Verify all preservation tests still pass
    - **Property 2: Preservation** - Non-Buggy Behaviors Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run all 5 preservation tests from step 2
    - **EXPECTED OUTCOME**: All tests PASS (confirms no regressions)
    - Confirm all tests still pass after fixes (no regressions)
    - _Requirements: 3.1-3.17 (all Preservation Requirements)_

- [ ] 4. Checkpoint - Ensure all tests pass
  - Ensure all exploration tests pass (confirms bugs fixed)
  - Ensure all preservation tests pass (confirms no regressions)
  - Ask the user if questions arise
