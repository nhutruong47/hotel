---
description: Complete booking workflow from user perspective
---

# Booking Workflow

## User Flow

### 1. Browse Rooms
- Go to homepage at http://localhost:8080
- View available rooms with prices and amenities
- Click "Đặt phòng" on desired room

### 2. Create Booking (Status: PENDING)
- Fill booking form:
  - Check-in date (must be today or future)
  - Check-out date (must be after check-in)
  - Guest name
  - Guest phone (optional)
  - Notes (optional)
- Submit booking
- System validates room availability
- Booking created with status `PENDING`
- Email notification sent to admin

### 3. Admin Approval
- Admin reviews booking at `/admin/bookings`
- Admin can:
  - **Approve**: Status → `AWAITING_PAYMENT`, payment deadline set (24h)
  - **Reject**: Status → `REJECTED`, user notified with reason

### 4. User Payment (Status: AWAITING_PAYMENT)
- User sees payment button in "Đặt phòng của tôi"
- User clicks "Thanh toán" 
- Payment page shows:
  - Booking details
  - Bank transfer info (simulated)
  - QR code
- User confirms payment
- Status → `CONFIRMED`

### 5. Booking Completion
- After checkout date, booking can be reviewed
- User can rate the room (1-5 stars) and leave comment

## Status Flow
```
PENDING → AWAITING_PAYMENT → CONFIRMED → COMPLETED
    ↓           ↓
 REJECTED    CANCELLED (by user or expired)
```

## Scheduled Jobs
- Payment deadline checker runs hourly
- Expired `AWAITING_PAYMENT` bookings auto-cancelled
