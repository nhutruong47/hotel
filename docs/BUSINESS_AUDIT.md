# Villa Booking Platform - Comprehensive Business & Technical Audit

**Date:** July 2026
**Auditors:** Principal Software Architect, Enterprise Business Analyst, QA Lead, Senior Full-Stack Engineer.

## Executive Summary
This audit evaluated the platform across business logic, technical architecture, security, and user experience. While the platform has a solid foundation (Spring Boot 3 + Next.js), several critical workflows are broken or incomplete, particularly around booking state machine transitions and enterprise readiness.

---

## Issue Log

### 1. Missing Core Booking State Transitions (CHECKED_IN / CHECKED_OUT)
- **Module**: `BookingService.java`, `AdminApi.java`, `BookingScheduler.java`
- **Current behavior**: Admin can `approve` a booking (sets state to `PAID`) and `complete` it (sets state to `COMPLETED`). The intermediate operational states `CHECKED_IN` and `CHECKED_OUT` are defined in `BookingStatus.java` but have no transition methods in `BookingService` or endpoints in `AdminApi`.
- **Expected enterprise behavior**: Receptionists must physically check-in guests (transition to `CHECKED_IN`) and check them out (transition to `CHECKED_OUT`).
- **Business impact**: Front-desk operations cannot be tracked. Cleaning/Housekeeping schedules cannot be automated because the system doesn't know if a guest is currently occupying the room.
- **Root cause**: Incomplete state machine implementation.
- **Recommended solution**: Implement `checkInBooking(id)` and `checkOutBooking(id)` in `BookingService` and expose them in `AdminApi`.
- **Priority**: **P0**

### 2. Broken Scheduler Auto-Completion Logic
- **Module**: `BookingScheduler.java`
- **Current behavior**: The `autoCompletePastStays` cron job specifically looks for bookings with status `CHECKED_OUT` and transitions them to `COMPLETED` when the checkout date passes. Since `CHECKED_OUT` is never set, the scheduler does nothing. Paid bookings remain as `PAID` forever.
- **Expected enterprise behavior**: Bookings that are `PAID` but pass check-in without showing up should become `NO_SHOW`. Bookings that are `CHECKED_OUT` should auto-transition to `COMPLETED`.
- **Business impact**: Stale data accumulation. Revenue recognition and analytics will fail because bookings never reach `COMPLETED` state.
- **Root cause**: Logic mismatch between scheduler queries and actual booking lifecycle.
- **Recommended solution**: Update scheduler to handle `PAID` -> `COMPLETED`/`NO_SHOW` auto-transitions based on elapsed dates.
- **Priority**: **P0**

### 3. Lack of Real Payment Gateway Integration
- **Module**: `PaymentApi.java`, `PaymentService.java`
- **Current behavior**: `PaymentApi.create` just creates a local DB record of a payment without communicating with an external acquirer (e.g., VNPay, Momo, Stripe). 
- **Expected enterprise behavior**: Payments should redirect to a secure payment gateway, and the system should rely on webhooks (IPN) to verify payment success before marking a booking as `PAID`.
- **Business impact**: Fraud risk. Manual reconciliation is required for every booking.
- **Root cause**: MVP approach deferred payment provider integration.
- **Recommended solution**: Implement a Payment Gateway Interface (e.g., `VNPayGateway`) and webhooks for asynchronous status updates.
- **Priority**: **P1**

### 4. Coarse Role Management (Missing Staff Roles)
- **Module**: `User.java`, `SecurityConfig.java`
- **Current behavior**: System only has `USER` and `ADMIN`.
- **Expected enterprise behavior**: A luxury villa platform requires granular RBAC: `RECEPTIONIST` (manages check-ins), `HOUSEKEEPING` (views room statuses), `MANAGER` (views reports), `ADMIN` (system config).
- **Business impact**: Security risk (Principle of Least Privilege violated). Receptionists shouldn't have access to global revenue reports.
- **Root cause**: Over-simplified role structure.
- **Recommended solution**: Add `STAFF` or `RECEPTIONIST` roles and update `@PreAuthorize` annotations across `AdminApi`.
- **Priority**: **P1**

### 5. No Dynamic / Seasonal Pricing Capability
- **Module**: `Room.java`, `BookingService.calculatePricing`
- **Current behavior**: `Room` has a static `pricePerNight`. 
- **Expected enterprise behavior**: Prices must fluctuate based on weekends, holidays, and high/low seasons.
- **Business impact**: Massive loss of potential revenue during peak seasons.
- **Root cause**: Lack of `RoomPricing` override table.
- **Recommended solution**: Introduce a `SeasonPricing` entity that maps date ranges to price multipliers.
- **Priority**: **P1**

### 6. Missing API Rate Limiting and Brute Force Protection
- **Module**: `AuthApi.java`, Spring Security
- **Current behavior**: Login and registration endpoints can be hammered infinitely.
- **Expected enterprise behavior**: Endpoints should be rate-limited (e.g., using Bucket4j). Accounts should temporarily lock after 5 failed login attempts.
- **Business impact**: Vulnerable to credential stuffing and DDoS.
- **Root cause**: Missing infrastructure middleware.
- **Recommended solution**: Implement `Bucket4j` rate limiting on `/api/v1/auth/*`.
- **Priority**: **P2**

### 7. UX Gap: No Client-side Polling for Payment Status
- **Module**: Frontend `BookingSuccess` / `Checkout`
- **Current behavior**: After creating a booking, user is left waiting or must manually refresh to see if Admin approved the bank transfer.
- **Expected enterprise behavior**: Real-time or periodic polling to update the UI when payment is received.
- **Business impact**: Poor user experience, increased support calls.
- **Root cause**: Lack of WebSocket or polling mechanism.
- **Recommended solution**: Implement SWR polling on the booking detail page for `PENDING_PAYMENT` bookings.
- **Priority**: **P2**

### 8. Session Management & JWT Rotation
- **Module**: `SecurityConfig.java`
- **Current behavior**: Uses basic `HttpSession`. No token rotation or strict invalidation cross-device.
- **Expected enterprise behavior**: For mobile/web scalability, either use secure JWTs with refresh tokens or Redis-backed clustered sessions.
- **Business impact**: Scalability limits if deploying multiple backend nodes behind a load balancer without sticky sessions.
- **Root cause**: Default Spring Session in-memory configuration.
- **Recommended solution**: Migrate session store to Redis or implement stateless JWT.
- **Priority**: **P3**

---

## Execution Plan (Fixes to Apply)

Based on the audit, we will execute the following implementations immediately:

1. **[P0]** Implement `checkIn` and `checkOut` API endpoints in backend + integrate them into `AdminApi`.
2. **[P0]** Fix `BookingScheduler.java` to correctly process `PAID` bookings that pass check-in/out dates.
3. **[P1]** Scaffold a structured Payment Gateway Webhook handler.
4. **[P2]** Add Bucket4j Rate Limiting for Auth APIs.
