# Bugfix Requirements Document

## Introduction

Logo "SmartPark" ở góc trái header trong trang public map (`/map`) hiện tại không có chức năng link. Khi người dùng chưa đăng nhập và bấm vào logo, không có gì xảy ra. Điều này không đáp ứng kỳ vọng của người dùng rằng logo thường là một link để quay về trang chủ hoặc trang đăng nhập.

Bug này ảnh hưởng đến trải nghiệm người dùng (UX) vì họ không thể điều hướng nhanh về trang login thông qua logo như thông lệ của các ứng dụng web.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN người dùng truy cập trang `/map` (public-map.html) THEN logo "SmartPark" ở header chỉ là một `<div>` thông thường không có link

1.2 WHEN người dùng bấm vào logo "SmartPark" ở header trang `/map` THEN không có hành động nào xảy ra (không chuyển trang)

### Expected Behavior (Correct)

2.1 WHEN người dùng truy cập trang `/map` (public-map.html) THEN logo "SmartPark" ở header SHALL là một link có thể click được trỏ đến `/login`

2.2 WHEN người dùng bấm vào logo "SmartPark" ở header trang `/map` THEN hệ thống SHALL chuyển hướng người dùng đến trang `/login`

### Unchanged Behavior (Regression Prevention)

3.1 WHEN người dùng xem trang `/map` THEN giao diện hiển thị của logo (icon 🅿, text "Smart" + "Park", màu sắc, kích thước) SHALL CONTINUE TO giữ nguyên như hiện tại

3.2 WHEN người dùng bấm vào các link khác trong header (nút "Đặt vé", nút "Đăng nhập") THEN các link này SHALL CONTINUE TO hoạt động bình thường như hiện tại

3.3 WHEN người dùng xem trang `/map` THEN tất cả các chức năng khác của trang (hiển thị sơ đồ bãi xe, thống kê, chuyển đổi giữa khu xe máy và ô tô) SHALL CONTINUE TO hoạt động bình thường
