package com.hsf.hotel.service;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username:noreply@hotel.com}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Gửi email xác thực tài khoản
     */
    public void sendVerificationEmail(User user) {
        if (mailSender == null) {
            System.out.println("📧 [DEMO] Verification email for: " + user.getEmail());
            System.out.println("📧 [DEMO] Link: " + baseUrl + "/verify?token=" + user.getVerificationToken());
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("user", user);
            context.setVariable("verificationLink", baseUrl + "/verify?token=" + user.getVerificationToken());

            String htmlContent = templateEngine.process("email/verification-email", context);

            sendHtmlEmail(user.getEmail(), "Xác thực tài khoản - Như Hotel", htmlContent);
            System.out.println("✅ Verification email sent to: " + user.getEmail());
        } catch (Exception e) {
            System.err.println("❌ Failed to send verification email: " + e.getMessage());
        }
    }

    /**
     * Thông báo Admin có booking mới cần duyệt
     */
    public void sendNewBookingNotificationToAdmin(Booking booking, String adminEmail) {
        if (mailSender == null) {
            System.out.println("📧 [DEMO] New booking notification to admin: " + adminEmail);
            System.out.println(
                    "📧 [DEMO] Booking ID: " + booking.getId() + ", Room: " + booking.getRoom().getRoomNumber());
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("booking", booking);
            context.setVariable("checkInDate", booking.getCheckInDate().format(DATE_FORMATTER));
            context.setVariable("checkOutDate", booking.getCheckOutDate().format(DATE_FORMATTER));
            context.setVariable("adminLink", baseUrl + "/admin/bookings");

            String htmlContent = templateEngine.process("email/new-booking-admin", context);

            sendHtmlEmail(adminEmail, "Booking mới cần duyệt #" + booking.getId(), htmlContent);
            System.out.println("✅ Admin notification sent for booking: " + booking.getId());
        } catch (Exception e) {
            System.err.println("❌ Failed to send admin notification: " + e.getMessage());
        }
    }

    /**
     * Thông báo User booking đã được duyệt
     */
    public void sendBookingApprovedEmail(Booking booking) {
        String userEmail = booking.getUser().getEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            System.out.println("⚠️ User has no email, skipping notification");
            return;
        }

        if (mailSender == null) {
            System.out.println("📧 [DEMO] Booking approved email to: " + userEmail);
            System.out.println("📧 [DEMO] Deadline: " + booking.getPaymentDeadline());
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("booking", booking);
            context.setVariable("checkInDate", booking.getCheckInDate().format(DATE_FORMATTER));
            context.setVariable("checkOutDate", booking.getCheckOutDate().format(DATE_FORMATTER));
            context.setVariable("paymentDeadline", booking.getPaymentDeadline().format(DATETIME_FORMATTER));
            context.setVariable("myBookingsLink", baseUrl + "/my-bookings");

            String htmlContent = templateEngine.process("email/booking-approved", context);

            sendHtmlEmail(userEmail, "Đặt phòng đã được duyệt #" + booking.getId(), htmlContent);
            System.out.println("✅ Booking approved email sent to: " + userEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send approval email: " + e.getMessage());
        }
    }

    /**
     * Thông báo User booking bị từ chối
     */
    public void sendBookingRejectedEmail(Booking booking) {
        String userEmail = booking.getUser().getEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            System.out.println("⚠️ User has no email, skipping notification");
            return;
        }

        if (mailSender == null) {
            System.out.println("📧 [DEMO] Booking rejected email to: " + userEmail);
            System.out.println("📧 [DEMO] Reason: " + booking.getRejectionReason());
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("booking", booking);
            context.setVariable("rejectionReason", booking.getRejectionReason());
            context.setVariable("roomsLink", baseUrl + "/");

            String htmlContent = templateEngine.process("email/booking-rejected", context);

            sendHtmlEmail(userEmail, "Đặt phòng bị từ chối #" + booking.getId(), htmlContent);
            System.out.println("✅ Booking rejected email sent to: " + userEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send rejection email: " + e.getMessage());
        }
    }

    /**
     * Thông báo booking bị hủy do không thanh toán
     */
    public void sendBookingCancelledEmail(Booking booking) {
        String userEmail = booking.getUser().getEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            System.out.println("⚠️ User has no email, skipping notification");
            return;
        }

        if (mailSender == null) {
            System.out.println("📧 [DEMO] Booking cancelled email to: " + userEmail);
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("booking", booking);
            context.setVariable("roomsLink", baseUrl + "/");

            String htmlContent = templateEngine.process("email/booking-cancelled", context);

            sendHtmlEmail(userEmail, "Đặt phòng đã bị hủy #" + booking.getId(), htmlContent);
            System.out.println("✅ Booking cancelled email sent to: " + userEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send cancellation email: " + e.getMessage());
        }
    }

    /**
     * Thông báo Admin khi user thanh toán thành công
     */
    public void sendPaymentReceivedNotificationToAdmin(Booking booking, String adminEmail) {
        if (mailSender == null) {
            System.out.println("📧 [DEMO] Payment received notification to admin: " + adminEmail);
            System.out.println("📧 [DEMO] Booking #" + booking.getId() + " paid at " + booking.getPaidAt());
            System.out.println("📧 [DEMO] Amount: " + booking.getTotalPrice() + " VNĐ");
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("booking", booking);
            context.setVariable("checkInDate", booking.getCheckInDate().format(DATE_FORMATTER));
            context.setVariable("checkOutDate", booking.getCheckOutDate().format(DATE_FORMATTER));
            context.setVariable("paidAt", booking.getPaidAt().format(DATETIME_FORMATTER));
            context.setVariable("adminLink", baseUrl + "/admin/bookings");

            String htmlContent = templateEngine.process("email/payment-received-admin", context);

            sendHtmlEmail(adminEmail, "💰 Thanh toán thành công #" + booking.getId(), htmlContent);
            System.out.println("✅ Payment notification sent to admin: " + adminEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send payment notification: " + e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}
