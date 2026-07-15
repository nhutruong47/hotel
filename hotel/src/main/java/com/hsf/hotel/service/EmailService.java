package com.hsf.hotel.service;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import com.hsf.hotel.exception.ExternalServiceException;

import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String baseUrl;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender,
                        @Value("${spring.mail.username:noreply@hotel.com}") String fromEmail,
                        @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.baseUrl = baseUrl;
    }

    public void sendVerificationEmail(User user) {
        throw new UnsupportedOperationException("Use sendVerificationEmail(user, rawToken)");
    }

    /**
     * Send the verification email using the raw token (the entity only stores
     * the SHA-256 hash, so the raw token must be threaded through here).
     */
    public void sendVerificationEmail(User user, String rawToken) {
        if (mailSender == null) {
            log.info("[demo] Verification email for {}", user.getEmail());
            return;
        }
        try {
            String verificationLink = baseUrl + "/verify?token=" + rawToken;
            String htmlContent = String.format("""
                <h2>Xác thực tài khoản</h2>
                <p>Xin chào %s,</p>
                <p>Vui lòng nhấn vào đường dẫn bên dưới để xác thực tài khoản của bạn:</p>
                <p><a href="%s">Xác thực tài khoản</a></p>
                <p>Cảm ơn bạn đã sử dụng dịch vụ của Như Hotel.</p>
                """, escapeHtml(user.getFullName()), verificationLink);
            sendHtmlEmail(user.getEmail(), "Xác thực tài khoản - Như Hotel", htmlContent);
        } catch (Exception e) {
            log.warn("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    public void sendNewBookingNotificationToAdmin(Booking booking, String adminEmail) {
        if (mailSender == null) return;
        try {
            String adminLink = baseUrl + "/admin/bookings";
            String htmlContent = String.format("""
                <h2>Booking mới cần duyệt #%d</h2>
                <p>Có một đặt phòng mới từ %s.</p>
                <p>Phòng: %s</p>
                <p>Từ: %s - Đến: %s</p>
                <p><a href="%s">Xem chi tiết</a></p>
                """,
                    booking.getId(),
                    escapeHtml(booking.getUser().getFullName()),
                    booking.getRoom().getRoomNumber(),
                    booking.getCheckInDate().format(DATE_FORMATTER),
                    booking.getCheckOutDate().format(DATE_FORMATTER),
                    adminLink);
            sendHtmlEmail(adminEmail, "Booking mới cần duyệt #" + booking.getId(), htmlContent);
        } catch (Exception e) {
            log.warn("Failed to send admin booking notification for #{}: {}", booking.getId(), e.getMessage());
        }
    }

    public void sendBookingApprovedEmail(Booking booking) {
        String userEmail = booking.getUser().getEmail();
        if (userEmail == null || userEmail.isEmpty() || mailSender == null) return;
        try {
            String myBookingsLink = baseUrl + "/my-bookings";
            String htmlContent = String.format("""
                <h2>Đặt phòng của bạn đã được duyệt</h2>
                <p>Xin chào %s,</p>
                <p>Đặt phòng #%d của bạn đã được duyệt. Vui lòng thanh toán trước %s.</p>
                <p>Phòng: %s (Từ %s đến %s)</p>
                <p><a href="%s">Xem đặt phòng và Thanh toán</a></p>
                """,
                    escapeHtml(booking.getUser().getFullName()),
                    booking.getId(),
                    booking.getPaymentDeadline() != null
                            ? booking.getPaymentDeadline().format(DATETIME_FORMATTER) : "ngay",
                    booking.getRoom().getRoomNumber(),
                    booking.getCheckInDate().format(DATE_FORMATTER),
                    booking.getCheckOutDate().format(DATE_FORMATTER),
                    myBookingsLink);
            sendHtmlEmail(userEmail, "Đặt phòng đã được duyệt #" + booking.getId(), htmlContent);
        } catch (Exception e) {
            log.warn("Failed to send booking approval email for #{}: {}", booking.getId(), e.getMessage());
        }
    }

    public void sendBookingRejectedEmail(Booking booking) {
        String userEmail = booking.getUser().getEmail();
        if (userEmail == null || userEmail.isEmpty() || mailSender == null) return;
        try {
            String htmlContent = String.format("""
                <h2>Đặt phòng của bạn đã bị từ chối</h2>
                <p>Xin chào %s,</p>
                <p>Rất tiếc, đặt phòng #%d của bạn đã bị từ chối.</p>
                <p>Lý do: %s</p>
                <p>Vui lòng liên hệ với chúng tôi để biết thêm chi tiết.</p>
                """,
                    escapeHtml(booking.getUser().getFullName()),
                    booking.getId(),
                    escapeHtml(booking.getRejectionReason()));
            sendHtmlEmail(userEmail, "Đặt phòng bị từ chối #" + booking.getId(), htmlContent);
        } catch (Exception e) {
            log.warn("Failed to send booking rejection email for #{}: {}", booking.getId(), e.getMessage());
        }
    }

    public void sendBookingCancelledEmail(Booking booking) {
        sendBookingCancelledEmail(booking, "Đã hủy đặt phòng");
    }

    /**
     * Cancellation email with an explicit reason line. Replaces the previous
     * one-size-fits-all "do payment timeout" message so customers receive a
     * body that matches the actor that cancelled the booking (themselves vs.
     * admin vs. system auto-expire).
     */
    public void sendBookingCancelledEmail(Booking booking, String reason) {
        String userEmail = booking.getUser().getEmail();
        if (userEmail == null || userEmail.isEmpty() || mailSender == null) return;
        try {
            String cancelledBy = booking.getCancelledBy() != null
                    ? booking.getCancelledBy() : "system";
            String bodyIntro;
            if (reason != null && reason.toLowerCase().contains("expired")) {
                bodyIntro = "Đặt phòng của bạn đã hết hạn thanh toán và bị hủy tự động.";
            } else if ("system-scheduler".equalsIgnoreCase(cancelledBy)) {
                bodyIntro = "Đặt phòng của bạn đã bị hủy bởi hệ thống.";
            } else if ("ADMIN".equalsIgnoreCase(cancelledBy)
                    || booking.getCancelledBy() != null && !"system".equals(cancelledBy)
                            && !"system-scheduler".equals(cancelledBy)
                            && booking.getUser().getUsername() != null
                            && !cancelledBy.equals(booking.getUser().getUsername())) {
                bodyIntro = "Đặt phòng của bạn đã bị hủy bởi quản trị viên.";
            } else {
                bodyIntro = "Đặt phòng của bạn đã được hủy theo yêu cầu của bạn.";
            }
            String htmlContent = String.format("""
                <h2>Đặt phòng đã bị hủy</h2>
                <p>Xin chào %s,</p>
                <p>%s</p>
                <p>Mã đặt phòng: #%d</p>
                <p>Lý do: %s</p>
                """,
                    escapeHtml(booking.getUser().getFullName()),
                    bodyIntro,
                    booking.getId(),
                    escapeHtml(reason != null ? reason : "Không có"));
            sendHtmlEmail(userEmail, "Đặt phòng đã bị hủy #" + booking.getId(), htmlContent);
        } catch (Exception e) {
            log.warn("Failed to send cancellation email for #{}: {}", booking.getId(), e.getMessage());
        }
    }

    /**
     * Rich payment-confirmed email to the customer. Triggered by
     * {@link BookingService} whenever a booking transitions to PAID (admin
     * approval, gateway webhook, or manual confirmation).
     */
    public void sendPaymentConfirmedToCustomer(Booking booking) {
        String userEmail = booking.getUser().getEmail();
        if (userEmail == null || userEmail.isEmpty() || mailSender == null) return;
        try {
            String myBookingsLink = baseUrl + "/my-bookings";
            String htmlContent = String.format("""
                <h2>Thanh toán thành công</h2>
                <p>Xin chào %s,</p>
                <p>Chúng tôi đã nhận được thanh toán <strong>%s VNĐ</strong> cho đặt phòng #%d.</p>
                <p>Phòng: %s</p>
                <p>Nhận phòng: %s</p>
                <p>Trả phòng: %s</p>
                <p>Giờ nhận phòng tiêu chuẩn: %s — Giờ trả phòng: %s</p>
                <p><a href="%s">Xem chi tiết đặt phòng</a></p>
                <p>Cảm ơn bạn đã chọn Như Hotel.</p>
                """,
                    escapeHtml(booking.getUser().getFullName()),
                    booking.getTotalPrice(),
                    booking.getId(),
                    booking.getRoom().getRoomNumber(),
                    booking.getCheckInDate().format(DATE_FORMATTER),
                    booking.getCheckOutDate().format(DATE_FORMATTER),
                    booking.getRoom().getCheckInTime(),
                    booking.getRoom().getCheckOutTime(),
                    myBookingsLink);
            sendHtmlEmail(userEmail, "Thanh toán thành công #" + booking.getId(), htmlContent);
        } catch (Exception e) {
            log.warn("Failed to send payment confirmation email for #{}: {}",
                    booking.getId(), e.getMessage());
        }
    }

    public void sendBookingExpiredEmail(Booking booking) {
        String userEmail = booking.getUser().getEmail();
        if (userEmail == null || userEmail.isEmpty() || mailSender == null) return;
        try {
            String htmlContent = String.format("""
                <h2>Đặt phòng đã hết hạn</h2>
                <p>Xin chào %s,</p>
                <p>Đặt phòng #%d của bạn đã hết hạn thanh toán và bị hủy tự động.</p>
                <p>Nếu bạn vẫn muốn đặt phòng, vui lòng tạo yêu cầu mới trên website.</p>
                """,
                    escapeHtml(booking.getUser().getFullName()), booking.getId());
            sendHtmlEmail(userEmail, "Đặt phòng đã hết hạn #" + booking.getId(), htmlContent);
        } catch (Exception e) {
            log.warn("Failed to send expiry email for #{}: {}", booking.getId(), e.getMessage());
        }
    }

    public void sendNoShowNotificationEmail(Booking booking) {
        String userEmail = booking.getUser().getEmail();
        if (userEmail == null || userEmail.isEmpty() || mailSender == null) return;
        try {
            String htmlContent = String.format("""
                <h2>Thông báo không đến nhận phòng</h2>
                <p>Xin chào %s,</p>
                <p>Đặt phòng #%d của bạn đã được ghi nhận là không đến nhận phòng.</p>
                <p>Theo chính sách khách sạn, bạn có thể không được hoàn lại một phần hoặc toàn bộ phí.</p>
                """,
                    escapeHtml(booking.getUser().getFullName()), booking.getId());
            sendHtmlEmail(userEmail, "Thông báo không đến nhận phòng #" + booking.getId(), htmlContent);
        } catch (Exception e) {
            log.warn("Failed to send no-show email for #{}: {}", booking.getId(), e.getMessage());
        }
    }

    public void sendPaymentReceivedNotificationToAdmin(Booking booking, String adminEmail) {
        if (mailSender == null) return;
        try {
            String htmlContent = String.format("""
                <h2>Thanh toán thành công #%d</h2>
                <p>Khách hàng %s đã thanh toán %s VNĐ cho đặt phòng #%d.</p>
                <p>Lúc: %s</p>
                """,
                    booking.getId(),
                    escapeHtml(booking.getUser().getFullName()),
                    booking.getTotalPrice(),
                    booking.getId(),
                    booking.getPaidAt() != null ? booking.getPaidAt().format(DATETIME_FORMATTER) : "N/A");
            sendHtmlEmail(adminEmail, "Thanh toán thành công #" + booking.getId(), htmlContent);
        } catch (Exception e) {
            log.warn("Failed to send payment notification for #{}: {}", booking.getId(), e.getMessage());
        }
    }

    public void sendPasswordResetEmail(User user) {
        throw new UnsupportedOperationException("Use sendPasswordResetEmail(user, rawToken)");
    }

    public void sendPasswordResetEmail(User user, String rawToken) {
        if (mailSender == null) return;
        try {
            String resetLink = baseUrl + "/reset-password?token=" + rawToken;
            String htmlContent = String.format("""
                <h2>Yêu cầu đặt lại mật khẩu</h2>
                <p>Xin chào %s,</p>
                <p>Nhấn vào link dưới đây để đặt lại mật khẩu của bạn:</p>
                <p><a href="%s">Đặt lại mật khẩu</a></p>
                """, escapeHtml(user.getFullName()), resetLink);
            sendHtmlEmail(user.getEmail(), "Yêu cầu đặt lại mật khẩu - Như Hotel", htmlContent);
        } catch (Exception e) {
            log.warn("Failed to send password reset email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    public void sendContactInquiry(String name, String email, String message) {
        if (mailSender == null) {
            log.info("[demo] Contact inquiry from {} <{}>: {}", name, email, message);
            return;
        }
        try {
            String htmlContent = String.format("""
                <h2>Liên hệ mới từ %s</h2>
                <p>Email: %s</p>
                <p>Tin nhắn:</p>
                <blockquote>%s</blockquote>
                """, escapeHtml(name), escapeHtml(email), escapeHtml(message));
            sendHtmlEmail(fromEmail, "[Contact] " + escapeHtml(name), htmlContent);
        } catch (Exception e) {
            throw new ExternalServiceException("Không thể gửi liên hệ: " + e.getMessage(), e);
        }
    }

    private static String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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
