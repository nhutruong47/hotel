package com.hsf.hotel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable Spring's scheduled task execution capability.
 * 
 * <p>Scheduler is used for:
 * <ul>
 *   <li>Expiring unpaid bookings</li>
 *   <li>Auto-completing overdue bookings</li>
 *   <li>Marking no-show bookings</li>
 *   <li>Cleaning up old audit logs</li>
 * </ul>
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Configuration is handled via @Scheduled annotations on individual methods
    // Additional customization can be added here if needed (ThreadPool, etc.)
}
