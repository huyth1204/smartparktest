# Tài Liệu Yêu Cầu Bugfix

## Giới Thiệu

Tài liệu này mô tả các lỗi nghiêm trọng trong hệ thống SmartPark (Spring Boot parking management) cần được sửa chữa. Các lỗi bao gồm:

1. **Luồng xác minh email sai** - Admin nhập password và gửi trong email, không an toàn
2. **render.yaml thiếu mail credentials** - Thiếu MAIL_USERNAME và MAIL_PASSWORD
3. **DATABASE_URL format sai** - Render PostgreSQL cần parse connectionString thành JDBC URL
4. **paymentCode=null trong createParkedVehicle** - Gây NullPointerException
5. **findAll() performance issue** - processPayment gọi findAll() rồi filter trong memory
6. **Booking.Gmail field sai** - Nên là "email" (camelCase) không phải "Gmail" (PascalCase)
7. **Thiếu checkIn validation** - Không validate ngày checkIn trong quá khứ
8. **UserService tạo BCryptPasswordEncoder mới** - Nên inject thay vì tạo mới mỗi lần

Các lỗi này ảnh hưởng đến bảo mật, hiệu năng, và độ ổn định của hệ thống.

## Phân Tích Lỗi

### 1. Hành Vi Hiện Tại (Lỗi)

#### 1.1 Luồng Xác Minh Email Không An Toàn

1.1 KHI admin tạo tài khoản nhân viên THÌ hệ thống yêu cầu nhập cả email và password

1.2 KHI admin submit form tạo tài khoản THÌ hệ thống gửi email chứa password dạng plaintext đến nhân viên

1.3 KHI nhân viên click link xác minh trong email THÌ hệ thống kích hoạt tài khoản ngay lập tức mà không yêu cầu nhân viên tự đặt mật khẩu

1.4 KHI admin tạo tài khoản THÌ hệ thống không hiển thị modal/popup "Đang chờ xác minh..." với polling để kiểm tra trạng thái

1.5 KHI token xác minh được tạo THÌ hệ thống đặt thời gian hết hạn là 24 giờ thay vì 5 phút

1.6 KHI token xác minh hết hạn THÌ hệ thống không tự động xóa tài khoản chưa xác minh

#### 1.2 Thiếu Mail Credentials trong render.yaml

1.7 KHI deploy lên Render THÌ hệ thống không có biến môi trường MAIL_USERNAME và MAIL_PASSWORD

1.8 KHI hệ thống cố gửi email THÌ Spring Mail không thể xác thực với SMTP server do thiếu credentials

#### 1.3 DATABASE_URL Format Sai cho Render PostgreSQL

1.9 KHI Render cung cấp DATABASE_URL dạng connectionString (postgres://user:pass@host:port/db) THÌ Spring Boot không thể parse được vì cần JDBC URL format (jdbc:postgresql://host:port/db)

1.10 KHI application-prod.properties sử dụng ${DATABASE_URL} trực tiếp THÌ hệ thống không thể kết nối database trên Render

#### 1.4 paymentCode=null Gây NullPointerException

1.11 KHI DataInitializer.createParkedVehicle() tạo booking với status="PENDING" THÌ hệ thống không set paymentCode

1.12 KHI processPayment() gọi b.getPaymentCode() trên booking có paymentCode=null THÌ hệ thống gặp NullPointerException khi gọi upper.contains(b.getPaymentCode())

#### 1.5 findAll() Performance Issue

1.13 KHI processPayment() cần tìm booking theo paymentCode THÌ hệ thống gọi repo.findAll() để load toàn bộ bookings vào memory

1.14 KHI có hàng nghìn bookings trong database THÌ hệ thống filter trong memory thay vì query database, gây chậm và tốn bộ nhớ

#### 1.6 Booking.Gmail Field Sai Naming Convention

1.15 KHI Booking entity định nghĩa field "Gmail" THÌ hệ thống vi phạm Java naming convention (nên là "email" camelCase)

1.16 KHI JPA generate column name THÌ hệ thống tạo column "gmail" thay vì "email", gây nhầm lẫn

#### 1.7 Thiếu checkIn Validation

1.17 KHI BookingService.createBooking() nhận checkIn date THÌ hệ thống không validate xem checkIn có nằm trong quá khứ hay không

1.18 KHI user tạo booking với checkIn trong quá khứ THÌ hệ thống chấp nhận và tạo booking không hợp lệ

#### 1.8 UserService Tạo BCryptPasswordEncoder Mới

1.19 KHI UserService khởi tạo THÌ hệ thống tạo instance mới của BCryptPasswordEncoder bằng `new BCryptPasswordEncoder()`

1.20 KHI có nhiều request đồng thời THÌ hệ thống không tận dụng được singleton bean đã được Spring quản lý, gây lãng phí tài nguyên

### 2. Hành Vi Mong Đợi (Đúng)

#### 2.1 Luồng Xác Minh Email An Toàn

2.1 KHI admin tạo tài khoản nhân viên THÌ hệ thống CHỈ yêu cầu nhập email (KHÔNG nhập password)

2.2 KHI admin submit form tạo tài khoản THÌ hệ thống PHẢI hiển thị modal/popup "Đang chờ xác minh..." với polling API để kiểm tra trạng thái xác minh

2.3 KHI hệ thống gửi email xác minh THÌ email PHẢI chứa link xác minh (hết hạn 5 phút) và KHÔNG chứa mật khẩu

2.4 KHI nhân viên click link xác minh THÌ hệ thống PHẢI mở trang đặt mật khẩu (set-password page)

2.5 KHI nhân viên submit mật khẩu mới THÌ hệ thống PHẢI lưu mật khẩu đã hash, kích hoạt tài khoản, và chuyển hướng về trang login

2.6 KHI token xác minh hết hạn sau 5 phút THÌ hệ thống PHẢI tự động xóa tài khoản chưa xác minh

2.7 KHI polling API kiểm tra trạng thái THÌ hệ thống PHẢI trả về trạng thái "verified" hoặc "pending" hoặc "expired"

#### 2.2 Thêm Mail Credentials vào render.yaml

2.8 KHI deploy lên Render THÌ render.yaml PHẢI chứa envVars cho MAIL_USERNAME và MAIL_PASSWORD

2.9 KHI hệ thống gửi email THÌ Spring Mail PHẢI sử dụng credentials từ environment variables để xác thực với SMTP server

#### 2.3 Parse DATABASE_URL Đúng Format

2.10 KHI Render cung cấp DATABASE_URL dạng connectionString THÌ hệ thống PHẢI parse thành JDBC URL format (jdbc:postgresql://host:port/db?user=xxx&password=xxx)

2.11 KHI application-prod.properties sử dụng DATABASE_URL THÌ hệ thống PHẢI kết nối thành công với PostgreSQL database trên Render

#### 2.4 Set paymentCode Cho Parked Vehicles

2.12 KHI DataInitializer.createParkedVehicle() tạo booking THÌ hệ thống PHẢI generate và set paymentCode (format: "SP" + 6 ký tự random)

2.13 KHI processPayment() gọi b.getPaymentCode() THÌ hệ thống PHẢI không gặp NullPointerException vì paymentCode luôn có giá trị

#### 2.5 Tối Ưu processPayment Query

2.14 KHI processPayment() cần tìm booking theo paymentCode THÌ hệ thống PHẢI sử dụng custom query method trong repository (ví dụ: findByPaymentCodeAndStatusNot)

2.15 KHI có hàng nghìn bookings trong database THÌ hệ thống PHẢI query trực tiếp từ database với WHERE clause thay vì load toàn bộ vào memory

#### 2.6 Đổi Tên Field Booking.Gmail Thành email

2.16 KHI Booking entity định nghĩa field email THÌ hệ thống PHẢI tuân thủ Java naming convention (camelCase)

2.17 KHI JPA generate column name THÌ hệ thống PHẢI tạo column "email" rõ ràng và dễ hiểu

#### 2.7 Thêm checkIn Validation

2.18 KHI BookingService.createBooking() nhận checkIn date THÌ hệ thống PHẢI validate checkIn không được nằm trong quá khứ

2.19 KHI checkIn nằm trong quá khứ THÌ hệ thống PHẢI throw IllegalArgumentException với message "Ngày check-in không được trong quá khứ"

#### 2.8 Inject BCryptPasswordEncoder vào UserService

2.20 KHI UserService khởi tạo THÌ hệ thống PHẢI inject BCryptPasswordEncoder bean thông qua constructor hoặc @Autowired

2.21 KHI có nhiều request đồng thời THÌ hệ thống PHẢI sử dụng cùng một singleton instance của BCryptPasswordEncoder để tối ưu hiệu năng

### 3. Hành Vi Không Đổi (Phòng Ngừa Regression)

#### 3.1 Luồng Xác Minh Email - Các Trường Hợp Khác

3.1 KHI admin tạo tài khoản KHÔNG có email THÌ hệ thống PHẢI TIẾP TỤC tạo tài khoản active ngay lập tức (không cần xác minh)

3.2 KHI token xác minh hợp lệ và chưa hết hạn THÌ hệ thống PHẢI TIẾP TỤC cho phép nhân viên xác minh tài khoản

3.3 KHI nhân viên đã xác minh tài khoản THÌ hệ thống PHẢI TIẾP TỤC cho phép đăng nhập bình thường

#### 3.2 Mail Service - Các Chức Năng Khác

3.4 KHI gửi email reset password THÌ hệ thống PHẢI TIẾP TỤC hoạt động bình thường với format email hiện tại

3.5 KHI email service gặp lỗi THÌ hệ thống PHẢI TIẾP TỤC log error và không crash application

#### 3.3 Database Connection - Môi Trường Local

3.6 KHI chạy ở môi trường local (không có DATABASE_URL) THÌ hệ thống PHẢI TIẾP TỤC sử dụng H2 in-memory database

3.7 KHI application.properties có spring.datasource.url THÌ hệ thống PHẢI TIẾP TỤC ưu tiên sử dụng config đó

#### 3.4 Booking Service - Các Chức Năng Khác

3.8 KHI createBooking() được gọi với tham số hợp lệ THÌ hệ thống PHẢI TIẾP TỤC tạo booking với paymentCode được generate

3.9 KHI processPayment() tìm thấy booking hợp lệ THÌ hệ thống PHẢI TIẾP TỤC cập nhật status="PAID" và lưu thông tin thanh toán

3.10 KHI getAll() được gọi THÌ hệ thống PHẢI TIẾP TỤC trả về danh sách bookings sắp xếp theo createdAt desc

#### 3.5 Booking Entity - Các Field Khác

3.11 KHI Booking entity có các field khác (customerName, licensePlate, vehicleType, etc.) THÌ hệ thống PHẢI TIẾP TỤC giữ nguyên tên và kiểu dữ liệu

3.12 KHI JPA persist Booking entity THÌ hệ thống PHẢI TIẾP TỤC tự động set createdAt trong @PrePersist

#### 3.6 Validation - Các Trường Hợp Khác

3.13 KHI createBooking() nhận checkOut sau checkIn THÌ hệ thống PHẢI TIẾP TỤC chấp nhận và tính phí đúng

3.14 KHI PricingStrategy calculate() được gọi THÌ hệ thống PHẢI TIẾP TỤC tính phí theo logic hiện tại

#### 3.7 UserService - Các Chức Năng Khác

3.15 KHI UserService.register() được gọi THÌ hệ thống PHẢI TIẾP TỤC hash password và lưu user

3.16 KHI UserService.login() được gọi THÌ hệ thống PHẢI TIẾP TỤC verify password bằng BCryptPasswordEncoder

3.17 KHI UserService.resetPassword() được gọi THÌ hệ thống PHẢI TIẾP TỤC hash password mới và cập nhật user
