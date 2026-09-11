# Nhu Villas Resort Platform - Comprehensive API Documentation

Version: `2.0.0-ENTERPRISE`  
Base URL: `http://localhost:8080/api` (Production: `https://api.nhuvillas.com/api`)  
Authentication Scheme: Bearer Token (`Authorization: Bearer <jwt_token>`) & Secure HttpOnly Cookie (`AUTH-TOKEN`).

---

## 1. Authentication & Security Architecture

### 1.1 RBAC Roles Matrix

| Role | Hierarchy Level | Capabilities |
| :--- | :---: | :--- |
| `ROLE_CUSTOMER` | 1 | Search villas, create bookings, checkout, view own booking history, submit reviews, chat with AI concierge. |
| `ROLE_STAFF` | 2 | View all bookings, check-in / check-out guests, view villa statuses, update cleaning schedules. |
| `ROLE_INSPECTOR` | 3 | Schedule drone/manual villa inspections, record telemetry & checklists, sign off quality clearance. |
| `ROLE_MANAGER` | 4 | Manage pricing, vouchers, approve maintenance requests, view revenue metrics and occupancy reports. |
| `ROLE_ADMIN` | 5 | Superuser: full system access, user management, audit logs, raw database backup, system configs. |

---

### 1.2 Standard Response Envelope

#### Success Response
```json
{
  "status": "success",
  "data": { ... },
  "message": "Operation completed successfully",
  "timestamp": "2026-09-11T10:00:00Z"
}
```

#### Error Response
```json
{
  "timestamp": "2026-09-11T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Voucher is expired or fully redeemed",
  "path": "/api/bookings"
}
```

---

## 2. Authentication Endpoints (`/api/auth`)

### 2.1 Register New Account
- **Endpoint**: `POST /api/auth/register`
- **Access**: Public
- **Request Body**:
```json
{
  "email": "customer@example.com",
  "password": "Password123!",
  "fullName": "Nguyen Van A",
  "phone": "+84901234567"
}
```
- **Response `201 Created`**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 101,
    "email": "customer@example.com",
    "fullName": "Nguyen Van A",
    "role": "ROLE_CUSTOMER"
  }
}
```

### 2.2 Login
- **Endpoint**: `POST /api/auth/login`
- **Access**: Public
- **Request Body**:
```json
{
  "email": "customer@example.com",
  "password": "Password123!"
}
```
- **Response `200 OK`**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 101,
    "email": "customer@example.com",
    "fullName": "Nguyen Van A",
    "role": "ROLE_CUSTOMER"
  }
}
```

### 2.3 Current User Profile
- **Endpoint**: `GET /api/auth/me`
- **Access**: `Authenticated`
- **Response `200 OK`**:
```json
{
  "id": 101,
  "email": "customer@example.com",
  "fullName": "Nguyen Van A",
  "phone": "+84901234567",
  "role": "ROLE_CUSTOMER",
  "avatarUrl": "https://images.unsplash.com/photo-..."
}
```

---

## 3. Rooms & Villa Catalog (`/api/rooms`)

### 3.1 Search & Filter Villas
- **Endpoint**: `GET /api/rooms`
- **Access**: Public
- **Query Parameters**:
  - `checkIn` (ISO Date: `2026-10-01`): Check-in date
  - `checkOut` (ISO Date: `2026-10-05`): Check-out date
  - `guests` (integer): Guest count capacity filter
  - `type` (string): `OCEAN_VIEW`, `BEACHFRONT`, `GARDEN_RETREAT`, `PRESIDENTIAL_SUITE`
  - `minPrice` / `maxPrice` (number): Price range filter in USD / VND
  - `page` / `size`: Pagination controls
- **Response `200 OK`**:
```json
{
  "content": [
    {
      "id": 1,
      "name": "Grand Oceanfront Pool Villa",
      "roomNumber": "V-101",
      "type": "BEACHFRONT",
      "price": 850.00,
      "capacity": 6,
      "status": "AVAILABLE",
      "imageUrl": "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b",
      "galleryImages": [
        "https://images.unsplash.com/photo-1540555700478-4be289fbecef",
        "https://images.unsplash.com/photo-1584132967334-10e028bd69f7"
      ],
      "amenities": ["Private Infinity Pool", "Ocean Sunset View", "Butler Service", "Jacuzzi"],
      "minimumStay": 2,
      "maximumStay": 30,
      "rating": 4.95,
      "reviewCount": 48
    }
  ],
  "totalPages": 1,
  "totalElements": 8
}
```

### 3.2 Get Villa Details
- **Endpoint**: `GET /api/rooms/{id}`
- **Access**: Public
- **Response `200 OK`**: Complete room entity with full metadata, policies, surroundings, and coordinates.

---

## 4. Bookings & Concurrency Lifecycle (`/api/bookings`)

### 4.1 Create Booking with Concurrency Lock
- **Endpoint**: `POST /api/bookings`
- **Access**: `ROLE_CUSTOMER`, `ROLE_STAFF`, `ROLE_ADMIN`
- **Engine**: Executes `PESSIMISTIC_WRITE` lock on the target room and checks overlapping intervals before persisting booking in `PENDING` state.
- **Request Body**:
```json
{
  "roomId": 1,
  "checkInDate": "2026-10-15",
  "checkOutDate": "2026-10-18",
  "numGuests": 4,
  "guestName": "Nguyen Van A",
  "guestEmail": "customer@example.com",
  "guestPhone": "+84901234567",
  "specialRequests": "Late check-in at 8 PM, extra baby crib requested",
  "voucherCode": "LUXURY10",
  "paymentMethod": "VNPAY"
}
```
- **Response `201 Created`**:
```json
{
  "id": 501,
  "bookingCode": "NHU-202610-8472",
  "status": "PENDING",
  "totalPrice": 2295.00,
  "discountAmount": 255.00,
  "finalAmount": 2040.00,
  "checkInDate": "2026-10-15",
  "checkOutDate": "2026-10-18",
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=..."
}
```

### 4.2 Booking State Transitions
- **`POST /api/bookings/{id}/confirm`** (`ROLE_STAFF`, `ROLE_ADMIN`): Confirms `PENDING` booking.
- **`POST /api/bookings/{id}/check-in`** (`ROLE_STAFF`, `ROLE_ADMIN`): Transitions to `CHECKED_IN`.
- **`POST /api/bookings/{id}/check-out`** (`ROLE_STAFF`, `ROLE_ADMIN`): Transitions to `CHECKED_OUT` and schedules cleaning/inspection.
- **`POST /api/bookings/{id}/cancel`** (`ROLE_CUSTOMER`, `ROLE_ADMIN`): Releases room reservation slot.

---

## 5. Payments & Webhooks (`/api/payments`)

### 5.1 Initialize Payment
- **Endpoint**: `POST /api/payments/create-intent`
- **Supported Gateways**: `VNPAY`, `STRIPE`, `MOMO`, `PAYPAL`, `CASH`

### 5.2 VNPay IPN Callback (Idempotent)
- **Endpoint**: `GET /api/payments/vnpay-return` / `GET /api/payments/vnpay-ipn`
- **Security**: Validates `vnp_SecureHash` HMAC-SHA512 checksum. Idempotent check ensures double callbacks do not duplicate receipts.

---

## 6. Villa Inspections & Drone Aerial Survey (`/api/inspections`)

### 6.1 List Inspections
- **Endpoint**: `GET /api/inspections`
- **Access**: `ROLE_INSPECTOR`, `ROLE_MANAGER`, `ROLE_ADMIN`
- **Query Parameters**: `roomId`, `status` (`SCHEDULED`, `IN_PROGRESS`, `PASSED`, `ACTION_REQUIRED`)
- **Response `200 OK`**:
```json
[
  {
    "id": 1,
    "roomId": 1,
    "roomNumber": "V-101",
    "roomName": "Grand Oceanfront Pool Villa",
    "inspectorId": 3,
    "inspectorName": "Captain Drone Tech",
    "inspectionType": "DRONE_AERIAL",
    "status": "PASSED",
    "scheduledAt": "2026-09-11T09:00:00",
    "completedAt": "2026-09-11T09:45:00",
    "overallScore": 98.5,
    "droneModel": "DJI Matrice 350 RTK",
    "droneBatteryStart": 100,
    "droneBatteryEnd": 78,
    "flightAltitudeMeters": 45.0,
    "thermalHotspotsDetected": 0,
    "structuralIntegrityScore": 99.0,
    "roofTileDefects": 0,
    "poolCleanlinessScore": 100.0,
    "hvacAcousticDb": 34.2,
    "mediaUrls": [
      "https://images.unsplash.com/photo-1540555700478-4be289fbecef"
    ],
    "notes": "Optimal roof thermal footprint, water crystal clear, zero defects detected."
  }
]
```

### 6.2 Schedule New Drone / Manual Inspection
- **Endpoint**: `POST /api/inspections`
- **Access**: `ROLE_INSPECTOR`, `ROLE_ADMIN`
- **Request Body**:
```json
{
  "roomId": 1,
  "inspectionType": "DRONE_AERIAL",
  "scheduledAt": "2026-09-12T08:00:00",
  "droneModel": "DJI Matrice 350 RTK",
  "flightAltitudeMeters": 45.0,
  "notes": "Pre-VIP arrival roof tile & infinity pool structural scan"
}
```

### 6.3 Complete Inspection & Sign-off
- **Endpoint**: `POST /api/inspections/{id}/complete`
- **Access**: `ROLE_INSPECTOR`, `ROLE_ADMIN`
- **Request Body**:
```json
{
  "status": "PASSED",
  "overallScore": 99.0,
  "droneBatteryStart": 100,
  "droneBatteryEnd": 81,
  "thermalHotspotsDetected": 0,
  "structuralIntegrityScore": 100.0,
  "poolCleanlinessScore": 98.0,
  "hvacAcousticDb": 33.5,
  "notes": "Quality sign-off granted for VIP check-in.",
  "mediaUrls": ["https://resort.assets/drone/scan_v101.jpg"]
}
```

---

## 7. Reviews & Ratings (`/api/reviews`)

### 7.1 Public Room Reviews
- **Endpoint**: `GET /api/reviews/room/{roomId}`
- **Access**: Public
- **Response `200 OK`**: Sanitized DTO without password hashes or internal identifiers.
```json
[
  {
    "id": 12,
    "roomId": 1,
    "userName": "Sophia Taylor",
    "userAvatar": "https://images.unsplash.com/photo-1494790108377-be9c29b29330",
    "rating": 5,
    "comment": "Breathtaking ocean view and unmatched butler service.",
    "createdAt": "2026-09-08T14:20:00"
  }
]
```

### 7.2 Submit Verified Review
- **Endpoint**: `POST /api/reviews`
- **Access**: `ROLE_CUSTOMER` (requires completed stay verification).

---

## 8. AI Concierge & Smart Recommender (`/api/ai`)

### 8.1 Natural Language Recommendation
- **Endpoint**: `POST /api/ai/chat`
- **Access**: Public / Authenticated
- **Request Body**:
```json
{
  "message": "I'm looking for a private villa with an infinity pool and sunset view for an anniversary with 2 guests."
}
```
- **Response `200 OK`**:
```json
{
  "reply": "I recommend our Grand Oceanfront Pool Villa (V-101). It features a private infinity pool facing direct west for spectacular sunsets, complete with complimentary champagne service.",
  "recommendedRooms": [1, 3],
  "intent": "ANNIVERSARY_ROMANTIC"
}
```

---

## 9. Admin Operations (`/api/admin`)

- `GET /api/admin/overview`: Consolidated KPI dashboard (revenue, occupancy rate, pending inspections, active bookings).
- `POST /api/admin/rooms`: Upsert villa with full extended attributes (15+ fields).
- `GET /api/admin/vouchers`: List promo vouchers with clean redemption counters.
- `GET /api/admin/users`: User management with RBAC assignment.
- `GET /api/admin/audit-logs`: System audit trail for security and compliance.
