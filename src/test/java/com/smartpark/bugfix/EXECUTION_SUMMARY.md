# Bug Condition Exploration Tests - Execution Summary

## Task Completed: 1. Write bug condition exploration tests

**Status**: ✅ COMPLETED

**Files Created**:
1. `src/test/java/com/smartpark/bugfix/BugConditionExplorationTest.java` - Main test class with 8 exploration tests
2. `src/test/resources/application-test.properties` - Test profile configuration
3. `src/test/java/com/smartpark/bugfix/README.md` - Detailed test documentation
4. `src/test/java/com/smartpark/bugfix/EXECUTION_SUMMARY.md` - This file

## What Was Implemented

### 8 Bug Condition Exploration Tests

All 8 tests have been implemented following the **Bug Condition Exploration** methodology. These tests are **EXPECTED TO FAIL** on unfixed code to confirm the bugs exist.

#### Test 1.1: Email Verification Security Test
- **Validates**: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6
- **Tests**: Password in email, 24-hour token expiry, immediate activation
- **Expected Failure**: Token expires in 24 hours (not 5 minutes), account activated immediately

#### Test 1.2: Mail Credentials Test
- **Validates**: Requirements 1.7, 1.8
- **Tests**: Missing MAIL_USERNAME, MAIL_PASSWORD, MAIL_HOST, MAIL_PORT in environment
- **Expected Failure**: All environment variables are null

#### Test 1.3: Database URL Parsing Test
- **Validates**: Requirements 1.9, 1.10
- **Tests**: Parsing postgres:// to jdbc:postgresql:// format
- **Expected Failure**: Parser method doesn't exist, returns null

#### Test 1.4: PaymentCode Null Test
- **Validates**: Requirements 1.11, 1.12
- **Tests**: Parked vehicles created without paymentCode
- **Expected Failure**: paymentCode is null, processPayment throws NPE

#### Test 1.5: FindAll Performance Test
- **Validates**: Requirements 1.13, 1.14
- **Tests**: processPayment uses findAll() instead of custom query
- **Expected Failure**: Query takes > 100ms, custom method doesn't exist

#### Test 1.6: Booking Field Naming Test
- **Validates**: Requirements 1.15, 1.16
- **Tests**: Booking has "Gmail" field instead of "email"
- **Expected Failure**: "email" field doesn't exist, "Gmail" field exists

#### Test 1.7: CheckIn Validation Test
- **Validates**: Requirements 1.17, 1.18
- **Tests**: createBooking accepts checkIn in the past
- **Expected Failure**: No exception thrown for past checkIn dates

#### Test 1.8: BCryptPasswordEncoder Instance Test
- **Validates**: Requirements 1.19, 1.20
- **Tests**: UserService creates new encoder instead of injecting
- **Expected Failure**: encoder field is final, no injection mechanism

## Test Design Principles

### 1. Scoped PBT Approach
For deterministic bugs, tests are scoped to concrete failing cases to ensure reproducibility:
- Bug 1: Specific token expiry time (24 hours vs 5 minutes)
- Bug 4: Specific null paymentCode scenario
- Bug 6: Specific field name "Gmail" vs "email"
- Bug 7: Specific past date scenario

### 2. Observation-Based Testing
Tests observe actual behavior on unfixed code:
- Use reflection to inspect private fields and methods
- Use mocks to verify method calls with specific parameters
- Measure performance metrics (time, memory)
- Check for existence of methods and classes

### 3. Expected Behavior Encoding
Tests encode the expected (correct) behavior:
- When tests FAIL on unfixed code → confirms bugs exist
- When tests PASS on fixed code → confirms bugs are fixed
- Same tests used for both exploration and validation

## How to Run Tests

### Run All Tests
```bash
./mvnw test -Dtest=BugConditionExplorationTest
```

### Run Individual Test
```bash
./mvnw test -Dtest=BugConditionExplorationTest#test_1_1_EmailVerificationSecurity_FailsOnUnfixedCode
```

### Run with Verbose Output
```bash
./mvnw test -Dtest=BugConditionExplorationTest -X
```

## Expected Outcomes

### Phase 1: BEFORE Fix (Current State)
All 8 tests should **FAIL**:
- ❌ Test 1.1: Token expiry assertion fails (24h ≠ 5min)
- ❌ Test 1.2: Environment variables are null
- ❌ Test 1.3: Parser method doesn't exist
- ❌ Test 1.4: paymentCode is null
- ❌ Test 1.5: Query is slow, method doesn't exist
- ❌ Test 1.6: "email" field doesn't exist
- ❌ Test 1.7: No exception thrown
- ❌ Test 1.8: encoder is final, not injected

### Phase 3: AFTER Fix (Future State)
All 8 tests should **PASS**:
- ✅ Test 1.1: Token expires in 5 minutes, redirects to set-password
- ✅ Test 1.2: Environment variables are configured
- ✅ Test 1.3: Parser converts postgres:// to jdbc:postgresql://
- ✅ Test 1.4: paymentCode is generated
- ✅ Test 1.5: Query is fast, custom method exists
- ✅ Test 1.6: "email" field exists (camelCase)
- ✅ Test 1.7: IllegalArgumentException thrown
- ✅ Test 1.8: encoder is injected via Spring

## Counterexamples Surfaced

Each test surfaces specific counterexamples demonstrating the bugs:

1. **Bug 1**: Token with expiryDate = now + 24 hours, account.active = true immediately
2. **Bug 2**: System.getenv("MAIL_USERNAME") = null
3. **Bug 3**: parseDatabaseUrl("postgres://...") = null
4. **Bug 4**: booking.paymentCode = null → NPE in processPayment
5. **Bug 5**: processPayment with 1000 bookings takes > 100ms
6. **Bug 6**: Booking.class.getDeclaredField("Gmail") exists
7. **Bug 7**: createBooking(pastDate) succeeds without exception
8. **Bug 8**: UserService.encoder is final, initialized with new

## Next Steps

1. **DO NOT** attempt to fix the tests or code when they fail
2. **DO NOT** modify the test assertions to make them pass
3. **WAIT** for Phase 3 (Implementation) to fix the actual bugs
4. **RE-RUN** these same tests after fixes to confirm bugs are resolved

## Notes

- Tests use Spring Boot Test framework (@SpringBootTest)
- EmailService is mocked to avoid sending real emails
- Tests use H2 in-memory database (test profile)
- All tests are deterministic and reproducible
- Tests follow the bug condition exploration methodology from the design document

## Test Coverage

**Requirements Validated**: 1.1 - 1.20 (all Bug Condition requirements)

**Sub-tasks Completed**:
- ✅ 1.1 Email Verification Security Test
- ✅ 1.2 Mail Credentials Test
- ✅ 1.3 Database URL Parsing Test
- ✅ 1.4 PaymentCode Null Test
- ✅ 1.5 FindAll Performance Test
- ✅ 1.6 Booking Field Naming Test
- ✅ 1.7 CheckIn Validation Test
- ✅ 1.8 BCryptPasswordEncoder Instance Test

**Total**: 8/8 sub-tasks completed ✅
