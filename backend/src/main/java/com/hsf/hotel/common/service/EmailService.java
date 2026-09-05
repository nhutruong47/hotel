package com.hsf.hotel.common.service;
import com.hsf.hotel.payment.model.Payment;

import com.hsf.hotel.exception.ExternalServiceException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Email service for sending transactional emails.
 * 
 * <p>Supports HTML email templates with:
 * <ul>
 *   <li>Account verification</li>
 *   <li>Password reset</li>
 *   <li>Booking confirmations</li>
 *   <li>Payment notifications</li>
 *   <li>Contact form submissions</li>
 * </ul>
 * 
 * <p>Falls back to logging when mail sender is not configured.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String HOTEL_NAME = "Nhu Villas";
    private static final String PRIMARY_COLOR = "#2D5016"; // brand-forest

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String baseUrl;
    private final boolean emailEnabled;

    public EmailService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${spring.mail.username:noreply@nhu-villas.com}") String fromEmail,
            @Value("${app.base-url:http://localhost:5173}") String baseUrl,
            @Value("${app.email.enabled:true}") boolean emailEnabled) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.baseUrl = baseUrl;
        this.emailEnabled = emailEnabled && mailSender != null;
        
        if (!this.emailEnabled) {
            log.info("Email service running in demo mode - emails will be logged only");
        }
    }

    /**
     * Check if email sending is enabled.
     */
    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    // ============== Account Emails ==============

    public void sendVerificationEmail(com.hsf.hotel.user.model.User user) {
        throw new UnsupportedOperationException("Use sendVerificationEmail(user, rawToken)");
    }

    public void sendVerificationEmail(com.hsf.hotel.user.model.User user, String rawToken) {
        String verificationLink = baseUrl + "/verify-email?token=" + rawToken;
        
        String html = buildHtmlTemplate("Xác thực tài khoản - " + HOTEL_NAME, """
            <h1>Xác thực tài khoản</h1>
            <p>Xin chào <strong>%s</strong>,</p>
            <p>Cảm ơn bạn đã đăng ký! Vui lòng nhấn nút bên dưới để xác thực tài khoản của bạn.</p>
            <div style="text-align: center; margin: 30px 0;">
                <a href="%s" class="btn">Xác thực tài khoản</a>
            </div>
            <p>Hoặc sao chép link này vào trình duyệt:</p>
            <p style="word-break: break-all; color: #666;">%s</p>
            <p style="color: #888; font-size: 12px;">Link này sẽ hết hạn sau 24 giờ.</p>
            """.formatted(
                escapeHtml(user.getFullName()),
                verificationLink,
                verificationLink
            ),
            """
            <p>Nếu bạn không thực hiện đăng ký này, vui lòng bỏ qua email này.</p>
            """
        );
        
        sendEmail(user.getEmail(), "Xác thực tài khoản - " + HOTEL_NAME, html);
    }

    public void sendPasswordResetEmail(com.hsf.hotel.user.model.User user) {
        throw new UnsupportedOperationException("Use sendPasswordResetEmail(user, rawToken)");
    }

    public void sendPasswordResetEmail(com.hsf.hotel.user.model.User user, String rawToken) {
        String resetLink = baseUrl + "/forgot-password?token=" + rawToken;
        
        String html = buildHtmlTemplate("Đặt lại mật khẩu - " + HOTEL_NAME, """
            <h1>Đặt lại mật khẩu</h1>
            <p>Xin chào <strong>%s</strong>,</p>
            <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
            <p>Nhấn nút bên dưới để tạo mật khẩu mới:</p>
            <div style="text-align: center; margin: 30px 0;">
                <a href="%s" class="btn">Đặt lại mật khẩu</a>
            </div>
            <p>Hoặc sao chép link này vào trình duyệt:</p>
            <p style="word-break: break-all; color: #666;">%s</p>
            """.formatted(
                escapeHtml(user.getFullName()),
                resetLink,
                resetLink
            ),
            """
            <p style="color: #888; font-size: 12px;">Link này sẽ hết hạn sau 1 giờ.</p>
            <p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>
            """
        );
        
        sendEmail(user.getEmail(), "Đặt lại mật khẩu - " + HOTEL_NAME, html);
    }

    // ============== Booking Emails ==============

    public void sendNewBookingNotificationToAdmin(com.hsf.hotel.booking.model.Booking booking, String adminEmail) {
        String adminLink = baseUrl + "/admin/bookings/" + booking.getId();
        
        String html = buildHtmlTemplate("Booking mới #" + booking.getId() + " - " + HOTEL_NAME, """
            <h1>Booking mới cần duyệt</h1>
            <div class="alert">
                <strong>Mã booking:</strong> #%d<br/>
                <strong>Khách hàng:</strong> %s<br/>
                <strong>Email:</strong> %s<br/>
                <strong>Điện thoại:</strong> %s
            </div>
            
            <h2>Chi tiết đặt phòng</h2>
            <table class="info-table">
                <tr><td><strong>Phòng:</strong></td><td>%s</td></tr>
                <tr><td><strong>Nhận phòng:</strong></td><td>%s</td></tr>
                <tr><td><strong>Trả phòng:</strong></td><td>%s</td></tr>
                <tr><td><strong>Số khách:</strong></td><td>%d</td></tr>
                <tr><td><strong>Tổng tiền:</strong></td><td class="price">%s</td></tr>
            </table>
            
            <div style="text-align: center; margin: 30px 0;">
                <a href="%s" class="btn">Xem chi tiết</a>
            </div>
            """.formatted(
                booking.getId(),
                escapeHtml(booking.getUser().getFullName()),
                escapeHtml(booking.getGuestEmail() != null ? booking.getGuestEmail() : "N/A"),
                escapeHtml(booking.getGuestPhone() != null ? booking.getGuestPhone() : "N/A"),
                booking.getRoom().getRoomNumber(),
                booking.getCheckInDate().format(DATE_FORMATTER),
                booking.getCheckOutDate().format(DATE_FORMATTER),
                booking.getGuests() != null ? booking.getGuests() : 1,
                formatCurrency(booking.getTotalPrice()),
                adminLink
            ),
            ""
        );
        
        sendEmail(adminEmail, "Booking mới #" + booking.getId() + " - Cần duyệt", html);
    }

    public void sendBookingApprovedEmail(com.hsf.hotel.booking.model.Booking booking) {
        String bookingLink = baseUrl + "/account/bookings/" + booking.getId();
        String deadline = booking.getPaymentDeadline() != null 
                ? booking.getPaymentDeadline().format(DATETIME_FORMATTER) 
                : "ngay";
        
        String html = buildHtmlTemplate("Đặt phòng #" + booking.getId() + " đã được duyệt", """
            <h1>Đặt phòng đã được duyệt!</h1>
            <p>Xin chào <strong>%s</strong>,</p>
            <p>Đặt phòng <strong>#%d</strong> của bạn đã được duyệt.</p>
            
            <div class="alert success">
                <strong>⏰ Thanh toán trước:</strong> %s
            </div>
            
            <h2>Chi tiết đặt phòng</h2>
            <table class="info-table">
                <tr><td><strong>Phòng:</strong></td><td>%s</td></tr>
                <tr><td><strong>Nhận phòng:</strong></td><td>%s</td></tr>
                <tr><td><strong>Trả phòng:</strong></td><td>%s</td></tr>
                <tr><td><strong>Tổng tiền:</strong></td><td class="price">%s</td></tr>
            </table>
            
            <div style="text-align: center; margin: 30px 0;">
                <a href="%s" class="btn">Thanh toán ngay</a>
            </div>
            """.formatted(
                escapeHtml(booking.getUser().getFullName()),
                booking.getId(),
                deadline,
                booking.getRoom().getRoomNumber(),
                booking.getCheckInDate().format(DATE_FORMATTER),
                booking.getCheckOutDate().format(DATE_FORMATTER),
                formatCurrency(booking.getTotalPrice()),
                bookingLink
            ),
            """
            <p>Cảm ơn bạn đã chọn <strong>%s</strong>!</p>
            """.formatted(HOTEL_NAME)
        );
        
        sendEmail(booking.getUser().getEmail(), "Đặt phòng #" + booking.getId() + " đã được duyệt", html);
    }

    public void sendBookingRejectedEmail(com.hsf.hotel.booking.model.Booking booking) {
        String html = buildHtmlTemplate("Đặt phòng #" + booking.getId() + " bị từ chối", """
            <h1>Đặt phòng bị từ chối</h1>
            <p>Xin chào <strong>%s</strong>,</p>
            <p>Rất tiếc, đặt phòng <strong>#%d</strong> của bạn đã bị từ chối.</p>
            
            <div class="alert error">
                <strong>Lý do:</strong> %s
            </div>
            
            <p>Nếu bạn có thắc mắc, vui lòng liên hệ với chúng tôi.</p>
            """.formatted(
                escapeHtml(booking.getUser().getFullName()),
                booking.getId(),
                escapeHtml(booking.getRejectionReason() != null ? booking.getRejectionReason() : "Không có")
            ),
            """
            <p>Chân thành cảm ơn,<br/><strong>%s</strong></p>
            """.formatted(HOTEL_NAME)
        );
        
        sendEmail(booking.getUser().getEmail(), "Đặt phòng #" + booking.getId() + " bị từ chối", html);
    }

    public void sendBookingCancelledEmail(com.hsf.hotel.booking.model.Booking booking) {
        sendBookingCancelledEmail(booking, "Đã hủy đặt phòng");
    }

    public void sendBookingCancelledEmail(com.hsf.hotel.booking.model.Booking booking, String reason) {
        String cancelledBy = booking.getCancelledBy() != null ? booking.getCancelledBy() : "hệ thống";
        String bodyIntro = buildCancellationIntro(cancelledBy, reason, booking);
        
        String html = buildHtmlTemplate("Đặt phòng #" + booking.getId() + " đã bị hủy", """
            <h1>Đặt phòng đã bị hủy</h1>
            <p>Xin chào <strong>%s</strong>,</p>
            <p>%s</p>
            
            <div class="alert warning">
                <strong>Mã đặt phòng:</strong> #%d<br/>
                <strong>Phòng:</strong> %s<br/>
                <strong>Ngày:</strong> %s - %s
            </div>
            
            %s
            """.formatted(
                escapeHtml(booking.getUser().getFullName()),
                bodyIntro,
                booking.getId(),
                booking.getRoom().getRoomNumber(),
                booking.getCheckInDate().format(DATE_FORMATTER),
                booking.getCheckOutDate().format(DATE_FORMATTER),
                buildRefundInfo(booking)
            ),
            """
            <p>Chân thành cảm ơn,<br/><strong>%s</strong></p>
            """.formatted(HOTEL_NAME)
        );
        
        sendEmail(booking.getUser().getEmail(), "Đặt phòng #" + booking.getId() + " đã bị hủy", html);
    }

    public void sendBookingExpiredEmail(com.hsf.hotel.booking.model.Booking booking) {
        String html = buildHtmlTemplate("Đặt phòng #" + booking.getId() + " đã hết hạn", """
            <h1>Đặt phòng đã hết hạn</h1>
            <p>Xin chào <strong>%s</strong>,</p>
            <p>Đặt phòng <strong>#%d</strong> của bạn đã hết hạn thanh toán và bị hủy tự động.</p>
            
            <p>Nếu bạn vẫn muốn đặt phòng, vui lòng tạo yêu cầu mới trên website.</p>
            """.formatted(
                escapeHtml(booking.getUser().getFullName()),
                booking.getId()
            ),
            """
            <div style="text-align: center; margin: 30px 0;">
                <a href="%s" class="btn">Đặt phòng mới</a>
            </div>
            <p>Chân thành cảm ơn,<br/><strong>%s</strong></p>
            """.formatted(baseUrl + "/villas", HOTEL_NAME)
        );
        
        sendEmail(booking.getUser().getEmail(), "Đặt phòng #" + booking.getId() + " đã hết hạn", html);
    }

    public void sendNoShowNotificationEmail(com.hsf.hotel.booking.model.Booking booking) {
        String html = buildHtmlTemplate("Thông báo không đến nhận phòng", """
            <h1>Thông báo không đến nhận phòng</h1>
            <p>Xin chào <strong>%s</strong>,</p>
            <p>Đặt phòng <strong>#%d</strong> của bạn đã được ghi nhận là không đến nhận phòng.</p>
            
            <div class="alert warning">
                Theo chính sách khách sạn, bạn có thể không được hoàn lại một phần hoặc toàn bộ phí đặt phòng.
            </div>
            
            <p>Vui lòng liên hệ với chúng tôi nếu có thắc mắc.</p>
            """.formatted(
                escapeHtml(booking.getUser().getFullName()),
                booking.getId()
            ),
            """
            <p>Chân thành cảm ơn,<br/><strong>%s</strong></p>
            """.formatted(HOTEL_NAME)
        );
        
        sendEmail(booking.getUser().getEmail(), "Thông báo không đến nhận phòng #" + booking.getId(), html);
    }

    // ============== Payment Emails ==============

    public void sendPaymentConfirmedToCustomer(com.hsf.hotel.booking.model.Booking booking) {
        String bookingLink = baseUrl + "/account/bookings/" + booking.getId();
        
        String html = buildHtmlTemplate("Thanh toán thành công - " + HOTEL_NAME, """
            <h1>Thanh toán thành công!</h1>
            <p>Xin chào <strong>%s</strong>,</p>
            <p>Chúng tôi đã nhận được thanh toán của bạn.</p>
            
            <div class="alert success">
                <strong>Đã thanh toán:</strong> %s VNĐ<br/>
                <strong>Mã đặt phòng:</strong> #%d
            </div>
            
            <h2>Chi tiết đặt phòng</h2>
            <table class="info-table">
                <tr><td><strong>Phòng:</strong></td><td>%s</td></tr>
                <tr><td><strong>Nhận phòng:</strong></td><td>%s (từ %s)</td></tr>
                <tr><td><strong>Trả phòng:</strong></td><td>%s (trước %s)</td></tr>
            </table>
            
            <div style="text-align: center; margin: 30px 0;">
                <a href="%s" class="btn">Xem chi tiết đặt phòng</a>
            </div>
            """.formatted(
                escapeHtml(booking.getUser().getFullName()),
                formatCurrency(booking.getTotalPrice()),
                booking.getId(),
                booking.getRoom().getRoomNumber(),
                booking.getCheckInDate().format(DATE_FORMATTER),
                booking.getRoom().getCheckInTime() != null ? booking.getRoom().getCheckInTime() : "14:00",
                booking.getCheckOutDate().format(DATE_FORMATTER),
                booking.getRoom().getCheckOutTime() != null ? booking.getRoom().getCheckOutTime() : "11:00",
                bookingLink
            ),
            """
            <p>Chúng tôi rất mong được đón tiếp bạn!<br/>
            <strong>%s</strong></p>
            """.formatted(HOTEL_NAME)
        );
        
        sendEmail(booking.getUser().getEmail(), "Thanh toán thành công #" + booking.getId(), html);
    }

    public void sendPaymentReceivedNotificationToAdmin(com.hsf.hotel.booking.model.Booking booking, String adminEmail) {
        String adminLink = baseUrl + "/admin/bookings/" + booking.getId();
        
        String html = buildHtmlTemplate("Thanh toán thành công #" + booking.getId(), """
            <h1>Thanh toán thành công!</h1>
            <div class="alert success">
                <strong>Đã thanh toán:</strong> %s VNĐ<br/>
                <strong>Mã đặt phòng:</strong> #%d
            </div>
            
            <h2>Thông tin khách hàng</h2>
            <table class="info-table">
                <tr><td><strong>Tên:</strong></td><td>%s</td></tr>
                <tr><td><strong>Email:</strong></td><td>%s</td></tr>
                <tr><td><strong>Điện thoại:</strong></td><td>%s</td></tr>
            </table>
            
            <h2>Chi tiết đặt phòng</h2>
            <table class="info-table">
                <tr><td><strong>Phòng:</strong></td><td>%s</td></tr>
                <tr><td><strong>Nhận phòng:</strong></td><td>%s</td></tr>
                <tr><td><strong>Trả phòng:</strong></td><td>%s</td></tr>
            </table>
            
            <div style="text-align: center; margin: 30px 0;">
                <a href="%s" class="btn">Xem chi tiết</a>
            </div>
            """.formatted(
                formatCurrency(booking.getTotalPrice()),
                booking.getId(),
                escapeHtml(booking.getUser().getFullName()),
                escapeHtml(booking.getGuestEmail() != null ? booking.getGuestEmail() : "N/A"),
                escapeHtml(booking.getGuestPhone() != null ? booking.getGuestPhone() : "N/A"),
                booking.getRoom().getRoomNumber(),
                booking.getCheckInDate().format(DATE_FORMATTER),
                booking.getCheckOutDate().format(DATE_FORMATTER),
                adminLink
            ),
            ""
        );
        
        sendEmail(adminEmail, "Thanh toán thành công #" + booking.getId() + " - " + HOTEL_NAME, html);
    }

    // ============== Contact Email ==============

    public void sendContactInquiry(String name, String email, String message) {
        String html = buildHtmlTemplate("Liên hệ mới từ " + escapeHtml(name), """
            <h1>Liên hệ mới</h1>
            
            <table class="info-table">
                <tr><td><strong>Tên:</strong></td><td>%s</td></tr>
                <tr><td><strong>Email:</strong></td><td>%s</td></tr>
            </table>
            
            <h2>Tin nhắn</h2>
            <div class="message-box">%s</div>
            """.formatted(
                escapeHtml(name),
                escapeHtml(email),
                escapeHtml(message).replace("\n", "<br/>")
            ),
            ""
        );
        
        sendEmail(fromEmail, "[Liên hệ] " + escapeHtml(name) + " - " + HOTEL_NAME, html);
    }

    // ============== Helper Methods ==============

    private void sendEmail(String to, String subject, String htmlContent) {
        if (!emailEnabled) {
            log.info("[DEMO EMAIL] To: {}, Subject: {}", to, subject);
            log.debug("[DEMO EMAIL] Content:\n{}", htmlContent);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent successfully to {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        } catch (MailException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildHtmlTemplate(String title, String mainContent, String footer) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f5f5f5; padding: 30px 15px;">
                    <tr>
                        <td align="center">
                            <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08);">
                                <!-- Header -->
                                <tr>
                                    <td style="background-color: %s; padding: 30px; text-align: center;">
                                        <h1 style="color: #ffffff; margin: 0; font-size: 24px; font-weight: 600;">%s</h1>
                                    </td>
                                </tr>
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 40px 30px; color: #333333; line-height: 1.6;">
                                        %s
                                    </td>
                                </tr>
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f8f8f8; padding: 20px 30px; text-align: center; color: #666666; font-size: 14px;">
                                        %s
                                        <p style="margin: 10px 0 0 0; color: #999999; font-size: 12px;">
                                            Email này được gửi tự động từ %s<br/>
                                            Vui lòng không reply trực tiếp vào email này.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
                <style>
                    .btn { display: inline-block; padding: 14px 28px; background-color: %s; color: #ffffff !important; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 16px; }
                    .btn:hover { background-color: #234012; }
                    .alert { padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .alert.success { background-color: #d4edda; border: 1px solid #c3e6cb; color: #155724; }
                    .alert.error { background-color: #f8d7da; border: 1px solid #f5c6cb; color: #721c24; }
                    .alert.warning { background-color: #fff3cd; border: 1px solid #ffc107; color: #856404; }
                    .info-table { width: 100%%; border-collapse: collapse; margin: 15px 0; }
                    .info-table td { padding: 10px 0; border-bottom: 1px solid #eeeeee; }
                    .info-table td:last-child { text-align: right; }
                    .info-table .price { font-size: 18px; font-weight: 600; color: %s; }
                    .message-box { background-color: #f8f8f8; padding: 20px; border-radius: 8px; border-left: 4px solid %s; }
                </style>
            </body>
            </html>
            """.formatted(
                title,
                PRIMARY_COLOR, HOTEL_NAME,
                mainContent,
                footer,
                HOTEL_NAME,
                PRIMARY_COLOR,
                PRIMARY_COLOR,
                PRIMARY_COLOR
            );
    }

    private String buildCancellationIntro(String cancelledBy, String reason, com.hsf.hotel.booking.model.Booking booking) {
        String intro;
        if (reason != null && reason.toLowerCase().contains("expired")) {
            intro = "Đặt phòng của bạn đã hết hạn thanh toán và bị hủy tự động.";
        } else if ("system-scheduler".equalsIgnoreCase(cancelledBy)) {
            intro = "Đặt phòng của bạn đã bị hủy bởi hệ thống.";
        } else if ("ADMIN".equalsIgnoreCase(cancelledBy) || 
                  (booking.getCancelledBy() != null && 
                   !cancelledBy.equalsIgnoreCase("system") &&
                   !cancelledBy.equalsIgnoreCase("system-scheduler") &&
                   !cancelledBy.equalsIgnoreCase(booking.getUser().getUsername()))) {
            intro = "Đặt phòng của bạn đã bị hủy bởi quản trị viên.";
        } else {
            intro = "Đặt phòng của bạn đã được hủy theo yêu cầu.";
        }
        return intro;
    }

    private String buildRefundInfo(com.hsf.hotel.booking.model.Booking booking) {
        if (booking.getRefundAmount() == null || booking.getRefundAmount().signum() <= 0) {
            return "<p><em>Không có hoàn tiền cho đặt phòng này.</em></p>";
        }
        return """
            <div class="alert warning">
                <strong>Hoàn tiền:</strong> %s VNĐ (%d%%)<br/>
                <small>Tiền hoàn sẽ được xử lý trong 7-14 ngày làm việc.</small>
            </div>
            """.formatted(
                formatCurrency(booking.getRefundAmount()),
                booking.getRefundPercentage() != null ? booking.getRefundPercentage() : 0
            );
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

    private static String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }
}
