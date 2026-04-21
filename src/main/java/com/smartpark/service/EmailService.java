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
}