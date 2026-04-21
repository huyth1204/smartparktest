package com.smartpark.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendResetEmail(String toEmail, String resetLink) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("🅿️ SmartPark – Đặt lại mật khẩu");
            helper.setText("""
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;
                            border:1px solid #ddd;border-radius:8px;padding:24px">
                  <h2 style="color:#1a73e8">🅿️ SmartPark</h2>
                  <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
                  <p>Nhấn nút bên dưới để đặt lại mật khẩu (link hết hạn sau <b>30 phút</b>):</p>
                  <a href="%s"
                     style="display:inline-block;margin:16px 0;padding:12px 24px;
                            background:#1a73e8;color:#fff;border-radius:6px;
                            text-decoration:none;font-weight:bold">
                    Đặt lại mật khẩu
                  </a>
                  <p style="color:#888;font-size:12px">
                    Nếu bạn không yêu cầu, hãy bỏ qua email này.
                  </p>
                </div>
            """.formatted(resetLink), true);

            mailSender.send(msg);
        } catch (MessagingException e) {
            System.err.println("Lỗi gửi email: " + e.getMessage());
        }
    }

    public void sendAccountVerificationEmail(String toEmail, String verifyLink, String username) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("🅿️ SmartPark – Xác nhận tài khoản nhân viên");
            helper.setText("""
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;
                            border:1px solid #ddd;border-radius:8px;padding:24px">
                  <h2 style="color:#6c5ce7">🅿️ SmartPark</h2>
                  <p>Chào mừng bạn đến với hệ thống quản lý bãi đỗ xe SmartPark!</p>
                  <p>Admin đã tạo tài khoản nhân viên cho bạn. Vui lòng xác nhận email và đặt mật khẩu để kích hoạt tài khoản.</p>
                  
                  <div style="background:#f0f4f8;border-radius:8px;padding:16px;margin:16px 0">
                    <p style="margin:4px 0"><strong>Tên đăng nhập:</strong> <code style="background:#fff;padding:4px 8px;border-radius:4px">%s</code></p>
                  </div>
                  
                  <p>Nhấn nút bên dưới để xác nhận email và đặt mật khẩu (link hết hạn sau <b>5 phút</b>):</p>
                  <a href="%s"
                     style="display:inline-block;margin:16px 0;padding:12px 24px;
                            background:#6c5ce7;color:#fff;border-radius:6px;
                            text-decoration:none;font-weight:bold">
                    ✓ Xác nhận và đặt mật khẩu
                  </a>
                  <p style="color:#888;font-size:12px">
                    Sau khi đặt mật khẩu, bạn có thể đăng nhập ngay.
                  </p>
                  <p style="color:#888;font-size:12px">
                    Nếu bạn không yêu cầu tài khoản này, hãy bỏ qua email này.
                  </p>
                </div>
            """.formatted(username, verifyLink), true);

            mailSender.send(msg);
        } catch (MessagingException e) {
            System.err.println("Lỗi gửi email xác nhận: " + e.getMessage());
        }
    }

    public void sendStaffResetEmail(String toEmail, String fullName, String resetLink) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("🅿️ SmartPark – Đặt lại mật khẩu nhân viên");
            helper.setText("""
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;
                            border:1px solid #ddd;border-radius:8px;padding:24px">
                  <h2 style="color:#1a73e8">🅿️ SmartPark</h2>
                  <p>Xin chào <strong>%s</strong>,</p>
                  <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản nhân viên của bạn.</p>
                  <p>Nhấn nút bên dưới để đặt lại mật khẩu (link hết hạn sau <b>30 phút</b>):</p>
                  <a href="%s"
                     style="display:inline-block;margin:16px 0;padding:12px 24px;
                            background:#1a73e8;color:#fff;border-radius:6px;
                            text-decoration:none;font-weight:bold">
                    Đặt lại mật khẩu
                  </a>
                  <p style="color:#888;font-size:12px">
                    Nếu bạn không yêu cầu, hãy bỏ qua email này.
                  </p>
                </div>
            """.formatted(fullName, resetLink), true);

            mailSender.send(msg);
        } catch (MessagingException e) {
            System.err.println("Lỗi gửi email reset staff: " + e.getMessage());
        }
    }
}