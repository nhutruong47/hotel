package com.hsf.hotel.service;

import com.hsf.hotel.config.RabbitMQConfig;
import com.hsf.hotel.dto.NotificationEvent;
import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.User;
import com.hsf.hotel.repository.BookingRepository;
import com.hsf.hotel.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final EmailService emailService;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public NotificationListener(EmailService emailService, BookingRepository bookingRepository, UserRepository userRepository) {
        this.emailService = emailService;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    @Transactional(readOnly = true)
    public void handleEmailNotification(NotificationEvent event) {
        log.info("Received Email Notification Event: type={}, targetId={}", event.getType(), event.getTargetId());
        
        try {
            switch (event.getType()) {
                case "VERIFICATION":
                    userRepository.findById(event.getUserId()).ifPresent(user -> 
                        emailService.sendVerificationEmail(user, event.getRawToken()));
                    break;
                case "PASSWORD_RESET":
                    userRepository.findById(event.getUserId()).ifPresent(user -> 
                        emailService.sendPasswordResetEmail(user, event.getRawToken()));
                    break;
                case "NEW_BOOKING_ADMIN":
                    bookingRepository.findById(event.getTargetId()).ifPresent(booking -> 
                        emailService.sendNewBookingNotificationToAdmin(booking, event.getAdminEmail()));
                    break;
                case "BOOKING_APPROVED":
                    bookingRepository.findById(event.getTargetId()).ifPresent(emailService::sendBookingApprovedEmail);
                    break;
                case "BOOKING_REJECTED":
                    bookingRepository.findById(event.getTargetId()).ifPresent(emailService::sendBookingRejectedEmail);
                    break;
                case "BOOKING_CANCELLED":
                    bookingRepository.findById(event.getTargetId()).ifPresent(booking -> 
                        emailService.sendBookingCancelledEmail(booking, event.getExtraData()));
                    break;
                case "PAYMENT_CUSTOMER":
                    bookingRepository.findById(event.getTargetId()).ifPresent(emailService::sendPaymentConfirmedToCustomer);
                    break;
                case "PAYMENT_ADMIN":
                    bookingRepository.findById(event.getTargetId()).ifPresent(booking -> 
                        emailService.sendPaymentReceivedNotificationToAdmin(booking, event.getAdminEmail()));
                    break;
                case "NO_SHOW":
                    bookingRepository.findById(event.getTargetId()).ifPresent(emailService::sendNoShowNotificationEmail);
                    break;
                case "CONTACT_INQUIRY":
                    // Extra data can hold JSON or we can use another DTO, but for now we skip complex contact form or map it differently
                    log.warn("CONTACT_INQUIRY async handling not implemented yet via NotificationEvent.");
                    break;
                default:
                    log.warn("Unknown notification event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error processing email notification event: {}", e.getMessage(), e);
        }
    }
}
