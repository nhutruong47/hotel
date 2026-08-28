package com.hsf.hotel.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingScheduler {

    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void autoCancelExpiredBookings() {
        System.out.println("⏰ System no longer uses AWAITING_PAYMENT status. Skipping auto-cancel.");
    }

    public void sendPaymentReminders() {
        System.out.println("📧 System no longer uses AWAITING_PAYMENT. Skipping reminders.");
    }
}
