# Bug Condition Exploration Tests - Phase 1

## Overview

This directory contains 8 bug condition exploration tests that are **EXPECTED TO FAIL** on unfixed code. These tests confirm that the 8 critical bugs exist in the SmartPark system.

**IMPORTANT**: DO NOT attempt to fix the tests or the code when they fail. The failures are intentional and confirm the bugs exist.

## Test Execution Instructions

To run all bug condition exploration tests:

```bash
./mvnw test -Dtest=BugConditionExplorationTest
```

To run a specific test:

```bash
./mvnw test -Dtest=BugConditionExplorationTest#test_1_1_EmailVerificationSecurity_FailsOnUnfixedCode
```

## Expected Test Results (BEFORE Fix)

All 8 tests should **FAIL** on unfixed code:

### 1.1 Email Verification Security Test ❌ FAILS
**Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6**

**Why it fails:**
- Token expires in 24 hours, not 5 minutes (assertion fails)
- Account is activated immediately without set-password step (assertion fails)
- Email contains password plaintext (verified via mock)

**Counterexample:**
- Creating account with email "test@example.com" and password "tempPassword123"
- Token expiry: `LocalDateTime.now().plusHours(24)` instead of `plusMinutes(5)`
- `verifyAccount()` sets `active=true` immediately instead of redirecting to set-password page

### 1.2 Mail Credentials Test ❌ FAILS
**Validates: Requirements 1.7, 1.8**

**Why it fails:**
- `MAIL_USERNAME` environment variable is null (assertion fails)
- `MAIL_PASSWORD` environment variable is null (assertion fails)
- `MAIL_HOST` environment variable is null (assertion fails)
- `MAIL_PORT` environment variable is null (assertion fails)

**Counterexample:**
- render.yaml does not define these environment variables
- Spring Mail cannot authenticate with SMTP server in production

### 1.3 Database URL Parsing Test ❌ FAILS
**Validates: Requirements 1.9, 1.10**

**Why it fails:**
- `parseDatabaseUrl()` method returns null (doesn't exist in unfixed code)
- No DatabaseConfig class to parse postgres:// to jdbc:postgresql:// format

**Counterexample:**
- Render provides: `postgres://user:pass@host:5432/db`
- Spring Boot needs: `jdbc:postgresql://host:5432/db?user=user&password=pass`
- No parsing logic exists to convert between formats

### 1.4 PaymentCode Null Test ❌ FAILS
**Validates: Requirements 1.11, 1.12**

**Why it fails:**
- Booking created with `paymentCode=null` (assertion fails)
- `processPayment()` throws NullPointerException when calling `b.getPaymentCode()`

**Counterexample:**
- `DataInitializer.createParkedVehicle()` creates booking without setting paymentCode
- Line in BookingServiceImpl: `upper.contains(b.getPaymentCode())` crashes with NPE

### 1.5 FindAll Performance Test ❌ FAILS
**Validates: Requirements 1.13, 1.14**

**Why it fails:**
- `processPayment()` takes > 100ms with 1000 bookings (assertion fails)
- `BookingRepository.findByPaymentCodeAndStatusNot()` method doesn't exist (NoSuchMethodException)

**Counterexample:**
- Current code: `repo.findAll().stream().filter(...)` loads all bookings into memory
- With 1000 bookings, query time is slow and memory usage is high
- No custom query method for direct database lookup

### 1.6 Booking Field Naming Test ❌ FAILS
**Validates: Requirements 1.15, 1.16**

**Why it fails:**
- Booking class has field named "Gmail" (PascalCase) instead of "email" (camelCase)
- Assertion `hasEmailField` fails because field doesn't exist
- Assertion `!hasGmailField` fails because "Gmail" field exists

**Counterexample:**
- `Booking.java` line 15: `private String Gmail;`
- Violates Java naming convention (should be camelCase)

### 1.7 CheckIn Validation Test ❌ FAILS
**Validates: Requirements 1.17, 1.18**

**Why it fails:**
- `createBooking()` accepts checkIn in the past without throwing exception
- No validation logic exists in `BookingServiceImpl.createBooking()`

**Counterexample:**
- Calling `createBooking()` with `checkIn = LocalDateTime.now().minusDays(1)`
- Booking is created successfully (no IllegalArgumentException thrown)
- Invalid data is persisted to database

### 1.8 BCryptPasswordEncoder Instance Test ❌ FAILS
**Validates: Requirements 1.19, 1.20**

**Why it fails:**
- `encoder` field is marked `final` (assertion fails)
- No constructor accepting BCryptPasswordEncoder exists (assertion fails)
- No @Autowired annotation on encoder field (assertion fails)

**Counterexample:**
- `UserService.java` line 20: `private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();`
- Creates new instance instead of injecting Spring-managed singleton bean

## Expected Test Results (AFTER Fix)

After implementing the fixes in Phase 3, all 8 tests should **PASS**:

- ✅ 1.1 Email Verification Security Test
- ✅ 1.2 Mail Credentials Test
- ✅ 1.3 Database URL Parsing Test
- ✅ 1.4 PaymentCode Null Test
- ✅ 1.5 FindAll Performance Test
- ✅ 1.6 Booking Field Naming Test
- ✅ 1.7 CheckIn Validation Test
- ✅ 1.8 BCryptPasswordEncoder Instance Test

## Test Methodology

These tests follow the **Bug Condition Exploration** methodology:

1. **Setup**: Create inputs that trigger the bug condition
2. **Act**: Execute the buggy code path
3. **Assert**: Verify that the expected (correct) behavior does NOT occur
4. **Result**: Test FAILS, confirming the bug exists

The same tests will be re-run after fixes are implemented. When they PASS, it confirms the bugs are fixed.

## Notes

- These tests use Spring Boot Test framework with @SpringBootTest
- EmailService is mocked to avoid sending real emails
- Tests use H2 in-memory database (test profile)
- Reflection is used to inspect private fields and methods
- Tests are designed to be deterministic and reproducible
