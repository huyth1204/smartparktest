# Email Verification and Critical Bugs Fix - Bugfix Design

## Overview

Tài liệu thiết kế này mô tả giải pháp kỹ thuật để sửa 8 lỗi nghiêm trọng trong hệ thống SmartPark:

1. **Luồng xác minh email không an toàn** - Admin nhập password và gửi plaintext trong email
2. **render.yaml thiếu mail credentials** - Thiếu MAIL_USERNAME và MAIL_PASSWORD
3. **DATABASE_URL format sai** - Render PostgreSQL connectionString không tương thích với Spring Boot
4. **paymentCode=null gây NullPointerException** - DataInitializer không set paymentCode cho parked vehicles
5. **findAll() performance issue** - processPayment load toàn bộ bookings vào memory
6. **Booking.Gmail field sai naming** - Vi phạm Java naming convention
7. **Thiếu checkIn validation** - Không validate ngày checkIn trong quá khứ
8. **UserService tạo BCryptPasswordEncoder mới** - Không inject singleton bean

Các lỗi này ảnh hưởng nghiêm trọng đến bảo mật (lỗi 1, 8), hiệu năng (lỗi 5), độ ổn định (lỗi 3, 4), và chất lượng code (lỗi 6, 7).

## Glossary

- **Bug_Condition (C)**: Điều kiện kích hoạt lỗi - khi các tình huống cụ thể xảy ra (admin tạo tài khoản có email, deploy lên Render, processPayment được gọi, etc.)
- **Property (P)**: Hành vi mong đợi sau khi sửa - hệ thống hoạt động đúng, an toàn, và hiệu quả
- **Preservation**: Các hành vi hiện tại phải được giữ nguyên - login, booking, payment processing cho các trường hợp không bị lỗi
- **AccountVerificationService**: Service trong `src/main/java/com/smartpark/service/AccountVerificationService.java` xử lý xác minh tài khoản
- **DashboardController**: Controller trong `src/main/java/com/smartpark/controller/DashboardController.java` xử lý admin dashboard
- **BookingServiceImpl**: Implementation trong `src/main/java/com/smartpark/service/impl/BookingServiceImpl.java` xử lý booking logic
- **DataInitializer**: Component trong `src/main/java/com/smartpark/DataInitializer.java` khởi tạo dữ liệu demo
- **UserService**: Service trong `src/main/java/com/smartpark/service/UserService.java` xử lý user authentication
- **StaffAccount**: Entity đại diện cho tài khoản nhân viên với các trường verified và active
- **AccountVerificationToken**: Entity lưu token xác minh với expiryDate
- **Booking**: Entity đại diện cho booking với paymentCode và các thông tin thanh toán

## Bug Details

### Bug Condition

Các lỗi xảy ra trong 8 tình huống khác nhau:

**Bug 1: Luồng Xác Minh Email Không An Toàn**
- Admin tạo tài khoản nhân viên với email
- Hệ thống yêu cầu nhập password và gửi plaintext trong email
- Nhân viên click link xác minh và tài khoản được kích hoạt ngay lập tức
- Token hết hạn sau 24 giờ thay vì 5 phút
- Không có cơ chế polling để kiểm tra trạng thái xác minh

**Bug 2: Thiếu Mail Credentials**
- Deploy lên Render mà không có MAIL_USERNAME và MAIL_PASSWORD trong render.yaml
- Spring Mail không thể xác thực với SMTP server

**Bug 3: DATABASE_URL Format Sai**
- Render cung cấp DATABASE_URL dạng `postgres://user:pass@host:port/db`
- Spring Boot cần JDBC URL format `jdbc:postgresql://host:port/db?user=xxx&password=xxx`
- application-prod.properties sử dụng ${DATABASE_URL} trực tiếp

**Bug 4: paymentCode=null**
- DataInitializer.createParkedVehicle() tạo booking với status="PENDING" nhưng không set paymentCode
- processPayment() gọi b.getPaymentCode() gây NullPointerException

**Bug 5: findAll() Performance Issue**
- processPayment() gọi repo.findAll() để load toàn bộ bookings vào memory
- Filter trong memory thay vì query database với WHERE clause

**Bug 6: Booking.Gmail Field Sai Naming**
- Field "Gmail" vi phạm Java naming convention (nên là "email" camelCase)
- JPA generate column "gmail" gây nhầm lẫn

**Bug 7: Thiếu checkIn Validation**
- BookingService.createBooking() không validate checkIn date
- Chấp nhận booking với checkIn trong quá khứ

**Bug 8: UserService Tạo BCryptPasswordEncoder Mới**
- UserService khởi tạo `new BCryptPasswordEncoder()` thay vì inject bean
- Không tận dụng singleton bean đã được Spring quản lý

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type SystemOperation
  OUTPUT: boolean
  
  RETURN (input.operation == "CREATE_ACCOUNT_WITH_EMAIL" AND input.passwordInEmail == true)
         OR (input.operation == "DEPLOY_TO_RENDER" AND input.mailCredentialsMissing == true)
         OR (input.operation == "CONNECT_DATABASE" AND input.databaseUrlFormat == "postgres://")
         OR (input.operation == "CREATE_PARKED_VEHICLE" AND input.paymentCode == null)
         OR (input.operation == "PROCESS_PAYMENT" AND input.usesFind
All == true)
         OR (input.operation == "DEFINE_BOOKING_FIELD" AND input.fieldName == "Gmail")
         OR (input.operation == "CREATE_BOOKING" AND input.checkInInPast == true)
         OR (input.operation == "INIT_USER_SERVICE" AND input.createsNewEncoder == true)
END FUNCTION
```

### Examples

**Bug 1 Example:**
- Admin tạo tài khoản "baove3" với email "staff@example.com" và password "temp123"
- Email gửi đến staff@example.com chứa: "Mật khẩu tạm: temp123"
- Nhân viên click link xác minh → tài khoản active ngay lập tức
- **Vấn đề**: Password bị lộ trong email, không có cơ chế đặt mật khẩu riêng

**Bug 2 Example:**
- Deploy lên Render với render.yaml hiện tại
- Hệ thống cố gửi email xác minh
- **Lỗi**: `AuthenticationFailedException: 535 Authentication failed`

**Bug 3 Example:**
- Render set DATABASE_URL = `postgres://user:pass@dpg-xxx.oregon-postgres.render.com:5432/smartparkdb`
- Spring Boot cố parse URL này
- **Lỗi**: `Cannot load driver class: org.postgresql.Driver`

**Bug 4 Example:**
- DataInitializer tạo parked vehicle với licensePlate "29A-12345"
- Booking được tạo với paymentCode=null
- processPayment() gọi `upper.contains(b.getPaymentCode())`
- **Lỗi**: `NullPointerException at BookingServiceImpl.processPayment()`

**Bug 5 Example:**
- Database có 10,000 bookings
- processPayment() gọi repo.findAll() → load 10,000 records vào memory
- Filter trong Java stream
- **Vấn đề**: Chậm, tốn bộ nhớ, không scale

**Bug 6 Example:**
- Booking entity có field `private String Gmail;`
- JPA generate column "gmail" trong database
- **Vấn đề**: Vi phạm naming convention, gây nhầm lẫn

**Bug 7 Example:**
- User tạo booking với checkIn = "2024-01-01" (trong quá khứ)
- Hệ thống chấp nhận và tạo booking
- **Vấn đề**: Dữ liệu không hợp lệ

**Bug 8 Example:**
- UserService khởi tạo với `private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();`
- Mỗi request tạo instance mới (nếu service không phải singleton)
- **Vấn đề**: Lãng phí tài nguyên, không tận dụng Spring DI

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Login flow cho staff và user phải tiếp tục hoạt động bình thường
- Booking creation với tham số hợp lệ phải tiếp tục tạo booking thành công
- Payment processing cho bookings có paymentCode hợp lệ phải tiếp tục cập nhật status="PAID"
- Email reset password phải tiếp tục hoạt động với format hiện tại
- Database connection ở môi trường local (H2) phải tiếp tục hoạt động
- Tất cả các field khác của Booking entity phải giữ nguyên tên và kiểu dữ liệu
- PricingStrategy calculation phải tiếp tục tính phí theo logic hiện tại
- UserService.register(), login(), resetPassword() phải tiếp tục hoạt động bình thường

**Scope:**
Tất cả các inputs không liên quan đến 8 lỗi trên phải hoạt động hoàn toàn bình thường. Bao gồm:
- Tạo tài khoản không có email (active ngay lập tức)
- Chạy ở môi trường local (không cần parse DATABASE_URL)
- Bookings được tạo qua createBooking() (có paymentCode)
- Các operations không liên quan đến 8 bugs

## Hypothesized Root Cause

Dựa trên phân tích code, các nguyên nhân gốc rễ là:

### 1. Luồng Xác Minh Email Không An Toàn

**Root Cause**: Thiết kế ban đầu yêu cầu admin nhập password và gửi trong email, không có cơ chế đặt mật khẩu riêng cho nhân viên.

**Evidence**:
- DashboardController.addAccount() yêu cầu @RequestParam password
- verificationService.sendVerificationEmail() nhận tempPassword parameter
- EmailService.sendAccountVerificationEmail() gửi password trong email HTML
- AccountVerificationService.verifyAccount() chỉ set verified=true và active=true, không có bước đặt mật khẩu

**Impact**: Password bị lộ trong email, vi phạm security best practices.

### 2. Thiếu Mail Credentials trong render.yaml

**Root Cause**: render.yaml không định nghĩa MAIL_USERNAME và MAIL_PASSWORD environment variables.

**Evidence**:
- render.yaml chỉ có DATABASE_URL, PORT, SPRING_PROFILES_ACTIVE
- application-prod.properties không có spring.mail.* configuration
- Spring Mail cần credentials để xác thực với SMTP server

**Impact**: Email service không hoạt động trên Render production.

### 3. DATABASE_URL Format Sai

**Root Cause**: Render PostgreSQL cung cấp connectionString format `postgres://` nhưng Spring Boot JDBC cần format `jdbc:postgresql://`.

**Evidence**:
- application-prod.properties: `spring.datasource.url=${DATABASE_URL}`
- Render docs: "DATABASE_URL is in the format postgres://user:password@host:port/database"
- Spring Boot expects: "jdbc:postgresql://host:port/database?user=xxx&password=xxx"

**Impact**: Application không thể kết nối database trên Render.

### 4. paymentCode=null Gây NullPointerException

**Root Cause**: DataInitializer.createParkedVehicle() tạo Booking với status="PENDING" nhưng không gọi generatePaymentCode().

**Evidence**:
```java
// DataInitializer.java line ~90
Booking booking = new Booking();
booking.setStatus("PENDING");
// Missing: booking.setPaymentCode(generatePaymentCode());
```

**Impact**: processPayment() crash khi gọi `b.getPaymentCode()` trên null.

### 5. findAll() Performance Issue

**Root Cause**: BookingServiceImpl.processPayment() sử dụng `repo.findAll()` thay vì custom query method.

**Evidence**:
```java
// BookingServiceImpl.java line ~50
Optional<Booking> found = repo.findAll().stream()
    .filter(b -> b.getPaymentCode() != null && upper.contains(b.getPaymentCode()))
    .findFirst();
```

**Impact**: Load toàn bộ bookings vào memory, không scale với large dataset.

### 6. Booking.Gmail Field Sai Naming

**Root Cause**: Developer đặt tên field "Gmail" (PascalCase) thay vì "email" (camelCase).

**Evidence**:
```java
// Booking.java line ~15
private String Gmail;
```

**Impact**: Vi phạm Java naming convention, JPA generate column "gmail" gây nhầm lẫn.

### 7. Thiếu checkIn Validation

**Root Cause**: BookingServiceImpl.createBooking() không validate checkIn date trước khi tạo booking.

**Evidence**:
```java
// BookingServiceImpl.java line ~30
public Booking createBooking(..., LocalDateTime checkIn, ...) {
    // Missing validation: if (checkIn.isBefore(LocalDateTime.now())) throw ...
    b.setCheckIn(checkIn);
}
```

**Impact**: Chấp nhận dữ liệu không hợp lệ (checkIn trong quá khứ).

### 8. UserService Tạo BCryptPasswordEncoder Mới

**Root Cause**: UserService khởi tạo `new BCryptPasswordEncoder()` thay vì inject bean từ Spring context.

**Evidence**:
```java
// UserService.java line ~20
private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
```

**Impact**: Không tận dụng singleton bean, lãng phí tài nguyên.

## Correctness Properties

Property 1: Bug Condition - Email Verification Security

_For any_ account creation operation where email is provided, the fixed system SHALL NOT send password in email, SHALL generate a 5-minute expiry token, SHALL redirect to set-password page after verification, and SHALL provide polling API for verification status.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7**

Property 2: Bug Condition - Mail Credentials Configuration

_For any_ deployment to Render, the fixed render.yaml SHALL include MAIL_USERNAME and MAIL_PASSWORD environment variables, enabling Spring Mail to authenticate with SMTP server.

**Validates: Requirements 2.8, 2.9**

Property 3: Bug Condition - Database URL Parsing

_For any_ Render PostgreSQL connection, the fixed system SHALL parse DATABASE_URL from `postgres://` format to `jdbc:postgresql://` format with user and password as query parameters.

**Validates: Requirements 2.10, 2.11**

Property 4: Bug Condition - PaymentCode Initialization

_For any_ parked vehicle creation in DataInitializer, the fixed system SHALL generate and set paymentCode, preventing NullPointerException in processPayment().

**Validates: Requirements 2.12, 2.13**

Property 5: Bug Condition - Payment Query Optimization

_For any_ payment processing operation, the fixed system SHALL use custom repository query method instead of findAll(), querying database with WHERE clause for efficient lookup.

**Validates: Requirements 2.14, 2.15**

Property 6: Bug Condition - Booking Field Naming

_For any_ Booking entity definition, the fixed system SHALL use "email" field name (camelCase) instead of "Gmail" (PascalCase), following Java naming convention.

**Validates: Requirements 2.16, 2.17**

Property 7: Bug Condition - CheckIn Date Validation

_For any_ booking creation operation, the fixed system SHALL validate checkIn date is not in the past, throwing IllegalArgumentException if validation fails.

**Validates: Requirements 2.18, 2.19**

Property 8: Bug Condition - BCryptPasswordEncoder Injection

_For any_ UserService initialization, the fixed system SHALL inject BCryptPasswordEncoder bean via constructor or @Autowired, utilizing Spring-managed singleton instance.

**Validates: Requirements 2.20, 2.21**

Property 9: Preservation - Account Creation Without Email

_For any_ account creation operation where email is NOT provided, the fixed system SHALL produce the same result as the original system, creating active account immediately without verification.

**Validates: Requirements 3.1**

Property 10: Preservation - Existing Authentication Flows

_For any_ login, register, or password reset operation not involving the 8 bugs, the fixed system SHALL produce the same result as the original system, preserving all existing authentication behaviors.

**Validates: Requirements 3.2, 3.3, 3.4, 3.5, 3.15, 3.16, 3.17**

Property 11: Preservation - Booking and Payment Operations

_For any_ booking creation or payment processing operation with valid inputs (not triggering bugs), the fixed system SHALL produce the same result as the original system, preserving booking creation, payment processing, and pricing calculation logic.

**Validates: Requirements 3.8, 3.9, 3.10, 3.13, 3.14**

Property 12: Preservation - Database and Configuration

_For any_ local development environment or non-Render deployment, the fixed system SHALL produce the same result as the original system, using H2 in-memory database and existing configuration.

**Validates: Requirements 3.6, 3.7**

Property 13: Preservation - Entity Fields and Lifecycle

_For any_ Booking entity persistence operation, the fixed system SHALL preserve all other fields (customerName, licensePlate, vehicleType, etc.) and @PrePersist behavior, only changing the "Gmail" field name to "email".

**Validates: Requirements 3.11, 3.12**

## Fix Implementation

### Changes Required

Giả sử phân tích root cause đúng, các thay đổi cần thiết:

#### Bug 1: Luồng Xác Minh Email An Toàn

**File**: `src/main/java/com/smartpark/controller/DashboardController.java`

**Function**: `addAccount()`

**Specific Changes**:
1. **Remove password parameter**: Xóa `@RequestParam String password` khỏi method signature
2. **Generate random token**: Tạo token ngẫu nhiên thay vì nhận password từ admin
3. **Update email call**: Gọi `verificationService.sendVerificationEmail(acc, baseUrl)` không có tempPassword parameter
4. **Add polling endpoint**: Tạo endpoint mới `@GetMapping("/admin/accounts/verify-status/{id}")` trả về JSON `{"status": "pending|verified|expired"}`
5. **Update success message**: Thay đổi message thành "Đã tạo tài khoản. Email xác minh đã được gửi. Đang chờ nhân viên xác minh..."

**File**: `src/main/java/com/smartpark/service/AccountVerificationService.java`

**Function**: `sendVerificationEmail()`, `verifyAccount()`

**Specific Changes**:
1. **Update method signature**: Xóa `String tempPassword` parameter từ `sendVerificationEmail()`
2. **Change token expiry**: Đổi `plusHours(24)` thành `plusMinutes(5)`
3. **Update verifyAccount()**: Thay vì set verified=true và active=true, chỉ validate token và redirect đến set-password page
4. **Add setPassword() method**: Tạo method mới nhận token và newPassword, validate token, hash password, set verified=true và active=true
5. **Add cleanup job**: Tạo @Scheduled method xóa tokens hết hạn và accounts chưa xác minh sau 5 phút

**File**: `src/main/java/com/smartpark/service/EmailService.java`

**Function**: `sendAccountVerificationEmail()`

**Specific Changes**:
1. **Remove password from email**: Xóa phần hiển thị "Mật khẩu tạm" trong email HTML
2. **Update email content**: Thay đổi nội dung thành "Nhấn nút bên dưới để xác nhận email và đặt mật khẩu (link hết hạn sau 5 phút)"
3. **Update method signature**: Xóa `String tempPassword` parameter

**File**: `src/main/java/com/smartpark/controller/DashboardController.java` (new endpoint)

**Function**: `setPasswordPage()`, `doSetPassword()`

**Specific Changes**:
1. **Add GET /set-password**: Nhận token, validate, hiển thị form đặt mật khẩu
2. **Add POST /set-password**: Nhận token và newPassword, gọi `verificationService.setPassword(token, newPassword)`, redirect về login

**File**: `src/main/resources/templates/set-password.html` (new file)

**Specific Changes**:
1. **Create new template**: Tạo form với input password và confirm password
2. **Add client-side validation**: Validate password strength và match confirmation

**File**: `src/main/resources/templates/admin-dashboard.html`

**Specific Changes**:
1. **Remove password input**: Xóa input field password khỏi form tạo tài khoản
2. **Add polling script**: Thêm JavaScript polling `/admin/accounts/verify-status/{id}` mỗi 2 giây
3. **Add modal**: Hiển thị modal "Đang chờ xác minh..." khi tạo tài khoản có email

#### Bug 2: Thêm Mail Credentials vào render.yaml

**File**: `render.yaml`

**Specific Changes**:
1. **Add MAIL_USERNAME**: Thêm `- key: MAIL_USERNAME` với value từ Render environment variable
2. **Add MAIL_PASSWORD**: Thêm `- key: MAIL_PASSWORD` với value từ Render environment variable
3. **Add MAIL_HOST**: Thêm `- key: MAIL_HOST` với value "smtp.gmail.com"
4. **Add MAIL_PORT**: Thêm `- key: MAIL_PORT` với value "587"

**File**: `src/main/resources/application-prod.properties`

**Specific Changes**:
1. **Add mail configuration**:
```properties
spring.mail.host=${MAIL_HOST}
spring.mail.port=${MAIL_PORT}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

#### Bug 3: Parse DATABASE_URL Đúng Format

**File**: `src/main/resources/application-prod.properties`

**Specific Changes**:
1. **Add custom parsing logic**: Thay vì `spring.datasource.url=${DATABASE_URL}`, sử dụng:
```properties
spring.datasource.url=${JDBC_DATABASE_URL:${DATABASE_URL}}
```
2. **Add note**: Comment giải thích Render cần set JDBC_DATABASE_URL manually hoặc parse trong code

**Alternative**: Tạo @Configuration class parse DATABASE_URL

**File**: `src/main/java/com/smartpark/config/DatabaseConfig.java` (new file)

**Specific Changes**:
1. **Create @Configuration class**: Parse DATABASE_URL từ environment
2. **Extract components**: Parse `postgres://user:pass@host:port/db` thành components
3. **Build JDBC URL**: Tạo `jdbc:postgresql://host:port/db?user=xxx&password=xxx`
4. **Set datasource**: Configure DataSource bean với JDBC URL

#### Bug 4: Set paymentCode Cho Parked Vehicles

**File**: `src/main/java/com/smartpark/DataInitializer.java`

**Function**: `createParkedVehicle()`

**Specific Changes**:
1. **Generate paymentCode**: Gọi `generatePaymentCode()` method (copy từ BookingServiceImpl)
2. **Set paymentCode**: Thêm `booking.setPaymentCode(generatedCode)` trước `bookingRepo.save()`
3. **Extract generatePaymentCode()**: Move method từ BookingServiceImpl thành utility method hoặc copy vào DataInitializer

#### Bug 5: Tối Ưu processPayment Query

**File**: `src/main/java/com/smartpark/repository/BookingRepository.java`

**Specific Changes**:
1. **Add custom query method**:
```java
Optional<Booking> findByPaymentCodeAndStatusNot(String paymentCode, String status);
```
2. **Add alternative method** (nếu cần contains):
```java
@Query("SELECT b FROM Booking b WHERE b.paymentCode = :code AND b.status != 'PAID'")
Optional<Booking> findByPaymentCodeNotPaid(@Param("code") String code);
```

**File**: `src/main/java/com/smartpark/service/impl/BookingServiceImpl.java`

**Function**: `processPayment()`

**Specific Changes**:
1. **Replace findAll()**: Thay `repo.findAll().stream().filter(...)` bằng `repo.findByPaymentCodeAndStatusNot(paymentCode, "PAID")`
2. **Extract paymentCode**: Parse paymentCode từ content trước khi query
3. **Update logic**: Vì query trả về Optional, không cần filter trong Java

#### Bug 6: Đổi Tên Field Booking.Gmail Thành email

**File**: `src/main/java/com/smartpark/model/Booking.java`

**Specific Changes**:
1. **Rename field**: Đổi `private String Gmail;` thành `private String email;`
2. **Update getter/setter**: Lombok tự động generate, không cần thay đổi
3. **Add migration**: Nếu cần, tạo Flyway/Liquibase migration rename column "gmail" → "email"

**Note**: Nếu database đã có data, cần migration script:
```sql
ALTER TABLE bookings RENAME COLUMN gmail TO email;
```

#### Bug 7: Thêm checkIn Validation

**File**: `src/main/java/com/smartpark/service/impl/BookingServiceImpl.java`

**Function**: `createBooking()`

**Specific Changes**:
1. **Add validation**: Thêm check đầu method:
```java
if (checkIn.isBefore(LocalDateTime.now())) {
    throw new IllegalArgumentException("Ngày check-in không được trong quá khứ");
}
```
2. **Add test**: Tạo unit test verify exception được throw

#### Bug 8: Inject BCryptPasswordEncoder vào UserService

**File**: `src/main/java/com/smartpark/service/UserService.java`

**Specific Changes**:
1. **Remove field initialization**: Xóa `private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();`
2. **Add @Autowired field**: Thêm `@Autowired private BCryptPasswordEncoder encoder;`
3. **Alternative - Constructor injection**:
```java
private final BCryptPasswordEncoder encoder;

public UserService(BCryptPasswordEncoder encoder, ...) {
    this.encoder = encoder;
    ...
}
```

## Testing Strategy

### Validation Approach

Testing strategy tuân theo two-phase approach:
1. **Exploratory Bug Condition Checking**: Surface counterexamples trên UNFIXED code để confirm root cause
2. **Fix Checking + Preservation Checking**: Verify fix hoạt động đúng và không break existing functionality

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples demonstrating bugs BEFORE implementing fixes. Confirm hoặc refute root cause analysis.

**Test Plan**: Viết tests simulate các bug conditions và run trên UNFIXED code để observe failures.

**Test Cases**:

1. **Email Verification Security Test** (will fail on unfixed code)
   - Tạo tài khoản với email
   - Verify email được gửi chứa password plaintext
   - Verify token expiry là 24 giờ thay vì 5 phút
   - Verify không có polling endpoint

2. **Mail Credentials Test** (will fail on unfixed code)
   - Mock Render environment (không có MAIL_USERNAME/MAIL_PASSWORD)
   - Cố gửi email
   - Verify AuthenticationFailedException được throw

3. **Database URL Parsing Test** (will fail on unfixed code)
   - Set DATABASE_URL = "postgres://user:pass@host:5432/db"
   - Cố connect database
   - Verify connection failure

4. **PaymentCode Null Test** (will fail on unfixed code)
   - Gọi DataInitializer.createParkedVehicle()
   - Gọi processPayment() với payment code
   - Verify NullPointerException được throw

5. **FindAll Performance Test** (will fail on unfixed code)
   - Tạo 10,000 bookings trong database
   - Measure time và memory usage của processPayment()
   - Verify performance issue (>1s, >100MB memory)

6. **Booking Field Naming Test** (will fail on unfixed code)
   - Inspect Booking.class
   - Verify field name là "Gmail" thay vì "email"

7. **CheckIn Validation Test** (will fail on unfixed code)
   - Gọi createBooking() với checkIn trong quá khứ
   - Verify booking được tạo thành công (không throw exception)

8. **BCryptPasswordEncoder Instance Test** (will fail on unfixed code)
   - Inspect UserService.class
   - Verify encoder được khởi tạo bằng `new` thay vì inject

**Expected Counterexamples**:
- Password plaintext trong email
- SMTP authentication failure
- Database connection failure với postgres:// URL
- NullPointerException khi processPayment()
- Slow query performance với findAll()
- Field name "Gmail" vi phạm convention
- Booking với checkIn trong quá khứ được chấp nhận
- BCryptPasswordEncoder không phải Spring bean

### Fix Checking

**Goal**: Verify rằng với tất cả inputs triggering bug conditions, fixed system produces expected behavior.

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := fixedSystem(input)
  ASSERT expectedBehavior(result)
END FOR
```

**Test Cases**:

1. **Email Verification Security - Fixed**
   - Tạo tài khoản với email (không nhập password)
   - Verify email KHÔNG chứa password
   - Verify token expiry là 5 phút
   - Verify polling endpoint trả về status
   - Click link xác minh → redirect đến set-password page
   - Submit password → account active

2. **Mail Credentials - Fixed**
   - Deploy với render.yaml có MAIL_USERNAME/MAIL_PASSWORD
   - Gửi email
   - Verify email được gửi thành công

3. **Database URL Parsing - Fixed**
   - Set DATABASE_URL = "postgres://user:pass@host:5432/db"
   - Connect database
   - Verify connection thành công

4. **PaymentCode Initialization - Fixed**
   - Gọi DataInitializer.createParkedVehicle()
   - Verify booking.paymentCode != null
   - Gọi processPayment() → không throw NullPointerException

5. **Payment Query Optimization - Fixed**
   - Tạo 10,000 bookings
   - Gọi processPayment()
   - Verify query time <100ms, memory usage <10MB

6. **Booking Field Naming - Fixed**
   - Inspect Booking.class
   - Verify field name là "email" (camelCase)

7. **CheckIn Validation - Fixed**
   - Gọi createBooking() với checkIn trong quá khứ
   - Verify IllegalArgumentException được throw

8. **BCryptPasswordEncoder Injection - Fixed**
   - Inspect UserService dependencies
   - Verify encoder được inject từ Spring context

### Preservation Checking

**Goal**: Verify rằng với tất cả inputs NOT triggering bug conditions, fixed system produces SAME result as original system.

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT originalSystem(input) = fixedSystem(input)
END FOR
```

**Testing Approach**: Property-based testing recommended vì:
- Generates many test cases automatically
- Catches edge cases manual tests might miss
- Provides strong guarantees behavior unchanged for non-buggy inputs

**Test Plan**: Observe behavior on UNFIXED code first, then write property-based tests capturing that behavior.

**Test Cases**:

1. **Account Creation Without Email Preservation**
   - Tạo tài khoản không có email
   - Verify account active ngay lập tức (giống unfixed code)

2. **Login Flow Preservation**
   - Login với staff account
   - Login với user account
   - Verify redirect đúng dashboard (giống unfixed code)

3. **Booking Creation Preservation**
   - Tạo booking với checkIn hợp lệ (trong tương lai)
   - Verify booking được tạo với paymentCode (giống unfixed code)

4. **Payment Processing Preservation**
   - processPayment() với booking có paymentCode hợp lệ
   - Verify status="PAID" và paidAt được set (giống unfixed code)

5. **Password Reset Preservation**
   - Gửi reset password email
   - Reset password với token hợp lệ
   - Verify password được update (giống unfixed code)

6. **Local Database Preservation**
   - Chạy ở môi trường local (không có DATABASE_URL)
   - Verify H2 in-memory database được sử dụng (giống unfixed code)

7. **Booking Entity Fields Preservation**
   - Persist Booking với tất cả fields
   - Verify customerName, licensePlate, vehicleType, etc. không đổi (giống unfixed code)

8. **Pricing Calculation Preservation**
   - Tạo booking với các loại xe khác nhau
   - Verify pricing calculation giống unfixed code

### Unit Tests

- Test AccountVerificationService.sendVerificationEmail() không gửi password
- Test AccountVerificationService.verifyAccount() redirect đến set-password page
- Test AccountVerificationService.setPassword() hash password và active account
- Test DatabaseConfig parse DATABASE_URL đúng format
- Test DataInitializer.createParkedVehicle() set paymentCode
- Test BookingRepository.findByPaymentCodeAndStatusNot() query đúng
- Test BookingServiceImpl.createBooking() throw exception với checkIn trong quá khứ
- Test UserService inject BCryptPasswordEncoder bean

### Property-Based Tests

- Generate random account creation requests (with/without email) → verify correct flow
- Generate random DATABASE_URL formats → verify parsing hoặc fallback
- Generate random booking data → verify paymentCode always set
- Generate random payment codes → verify query performance <100ms
- Generate random checkIn dates → verify validation logic
- Generate random user operations → verify BCryptPasswordEncoder usage

### Integration Tests

- Test full email verification flow: create account → receive email → click link → set password → login
- Test full deployment flow: deploy to Render → send email → verify SMTP authentication
- Test full database connection flow: connect to Render PostgreSQL → query data
- Test full payment flow: create parked vehicle → process payment → verify status update
- Test full booking flow: create booking → validate checkIn → calculate price → save
