# 🅿️ SmartPark – Hệ thống quản lý bãi đỗ xe

## Chức năng hiện tại
- Đặt vé giữ xe trực tuyến
- Sinh mã thanh toán + QR VietQR
- Tự động xác nhận khi nhận webhook từ SePay
- Popup thông báo thành công real-time

---

## ⚡ Chạy local (2 bước)

```bash
# 1. Sửa thông tin ngân hàng trong file:
#    src/main/resources/application.properties
#    → app.bank.account, app.bank.owner, app.bank.name

# 2. Chạy
mvn spring-boot:run
# Truy cập: http://localhost:8080
```

---

## 🚀 Deploy lên Render (có URL thật)

### Bước 1 – Push lên GitHub
```bash
git init
git add .
git commit -m "init smartpark"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/smartpark.git
git push -u origin main
```

### Bước 2 – Tạo Web Service trên Render
1. Vào **render.com** → **New** → **Web Service**
2. Kết nối repo GitHub vừa push
3. Điền thông tin:
   - **Runtime:** Java
   - **Build Command:** `mvn clean package -DskipTests`
   - **Start Command:** `java -jar target/smartpark-0.0.1-SNAPSHOT.jar`
   - **Plan:** Free
4. **Deploy** → chờ ~3 phút

→ Render sẽ cho bạn URL: `https://smartpark-xxxx.onrender.com`

### Bước 3 – Đăng ký webhook SePay
1. Vào **sepay.vn** → Cài đặt → Webhook
2. Điền URL: `https://smartpark-xxxx.onrender.com/webhook/sepay`
3. Lưu lại

**Xong! Từ giờ khách chuyển khoản đúng nội dung → tự động xác nhận.**

---

## Cấu trúc project
```
src/main/java/com/smartpark/
├── SmartparkApplication.java   ← entry point
├── model/Booking.java          ← entity
├── repository/BookingRepository.java
├── service/BookingService.java ← business logic
└── controller/
    ├── BookingController.java  ← trang web
    └── WebhookController.java  ← nhận SePay callback
```

---

## Sau này mở rộng thêm
- [ ] Dashboard doanh thu
- [ ] Quản lý xe ra/vào
- [ ] Phân quyền nhân viên / quản lý
- [ ] Kết nối barrier phần cứng
- [ ] Chuyển sang MySQL thật (thay H2)
