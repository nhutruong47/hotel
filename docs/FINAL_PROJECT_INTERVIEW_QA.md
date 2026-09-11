# Nhu Villas Resort Platform - Comprehensive Final Defense & Technical Interview Guide

A 360-degree technical defense and mastery guide covering architecture, concurrency, database design, security/RBAC, real-time messaging, drone survey workflows, and frontend performance for the **Nhu Villas Resort Management Platform**.

---

## 🎯 Table of Contents
1. [Enterprise Architecture & System Design](#1-enterprise-architecture--system-design)
2. [Concurrency Control & Booking Race Conditions](#2-concurrency-control--booking-race-conditions)
3. [Database Design, Migrations & PostgreSQL Schema](#3-database-design-migrations--postgresql-schema)
4. [Security, Authentication & Role-Based Access Control (RBAC)](#4-security-authentication--role-based-access-control-rbac)
5. [Payment Integration, Webhooks & Idempotency](#5-payment-integration-webhooks--idempotency)
6. [Resilience, Caching & Message Queuing (RabbitMQ & Redis)](#6-resilience-caching--message-queuing-rabbitmq--redis)
7. [Inspector & Drone Aerial Survey Workflow](#7-inspector--drone-aerial-survey-workflow)
8. [Frontend Architecture (Next.js 15 App Router & React 19)](#8-frontend-architecture-nextjs-15-app-router--react-19)
9. [DevOps, Docker, Health Checks & Observability](#9-devops-docker-health-checks--observability)
10. [Scenario-Based Behavioral & Live Defense Questions](#10-scenario-based-behavioral--live-defense-questions)

---

## 1. Enterprise Architecture & System Design

### Q1.1: Can you describe the overall architecture of Nhu Villas and why this decoupled stack was chosen?
**Answer:**
Nhu Villas is built as a cloud-native, decoupled enterprise system comprising:
1. **Frontend**: Next.js 15 (React 19, TypeScript, TailwindCSS, Lucide Icons) leveraging Server-Side Rendering (SSR) for public villa catalogs and dynamic Client Components for interactive workflows (interactive room customizer, inspection checklists, real-time drone telemetry, admin dashboards).
2. **Backend**: Spring Boot 3.4+ (Java 21) following Domain-Driven Hexagonal/Layered design (`auth`, `room`, `booking`, `payment`, `review`, `notification`, `admin`, `ai`).
3. **Primary Relational Store**: PostgreSQL 17 with Flyway versioned migrations (V1 to V11), strict referential integrity, and composite GiST/B-tree indexes.
4. **Caching & Async Messaging**: Redis for multi-tier caching (room listings, session states) with automatic in-memory fallback; RabbitMQ for event-driven asynchronous processing (email notifications, audit trails, drone flight telemetry streams).

**Rationale**:
- Decoupling enables independent scaling: high read-throughput for catalog discovery via CDN/SSR without loading transactional booking DB nodes.
- Strong type safety end-to-end (TypeScript on client, Java 21 records & strong typing on server).
- Clean API contract prevents entity leakage to client browsers.

---

### Q1.2: How do you prevent Entity/DTO leakage across boundaries?
**Answer:**
In standard monolithic ORMs, exposing `@Entity` classes directly in REST controllers causes serious security vulnerabilities (e.g., exposing `passwordHash`, `verificationToken`, internal audit fields) and runtime exceptions (`LazyInitializationException`, circular serialization loops).
In Nhu Villas:
- Every API endpoint returns explicit record DTOs or clean projection classes (e.g., `ReviewDetailResponse`, `AdminVoucherResponse`, `AdminUserResponse`, `InspectionDto`).
- Sensitive fields like credentials and internal foreign keys are scrubbed before reaching serialization layers.
- JPA entities are confined strictly to service/repository layers.

---

## 2. Concurrency Control & Booking Race Conditions

### Q2.1: How does Nhu Villas prevent the "Double Booking" problem when two users attempt to book the same villa for overlapping dates simultaneously?
**Answer:**
We implement a multi-layered defense strategy:
1. **Pessimistic Write Locking (`LockModeType.PESSIMISTIC_WRITE`)**:
   During the transaction in `BookingService.createBooking()`, the target `Room` record is acquired with `SELECT ... FOR UPDATE`:
   ```java
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT r FROM Room r WHERE r.id = :id")
   Optional<Room> findByIdWithPessimisticLock(@Param("id") Long id);
   ```
2. **Overlap Query Check**:
   Within the exclusive lock boundary, the system verifies date overlaps:
   ```sql
   SELECT COUNT(b) FROM Booking b 
   WHERE b.room.id = :roomId 
     AND b.status IN ('CONFIRMED', 'PENDING', 'CHECKED_IN')
     AND NOT (b.checkOutDate <= :newCheckIn OR b.checkInDate >= :newCheckOut)
   ```
3. **Database Exclusion Constraint / Unique Reservation Guard**:
   PostgreSQL enforces referential uniqueness so that no two non-cancelled bookings can occupy the identical room-date block.
4. **Result**: The second transaction waits for lock acquisition, executes the overlap query, sees `count > 0`, and is cleanly rejected with `409 Conflict` or a descriptive business error.

---

### Q2.2: Why choose Pessimistic Locking over Optimistic Locking (`@Version`) for room reservations?
**Answer:**
- **High Contention During Sales/Holidays**: Under high concurrency (e.g., flash sales or peak holiday bookings), optimistic locking causes numerous `OptimisticLockException` rollback retries, degrading database performance and providing poor user experience.
- **Fail-Fast Semantics**: Pessimistic write locks serialize access on the specific room row for a sub-50ms transaction window, guaranteeing immediate deterministic pass/fail outcomes.

---

## 3. Database Design, Migrations & PostgreSQL Schema

### Q3.1: How are database schema changes managed across team environments and production?
**Answer:**
- We utilize **Flyway** with forward-only versioned migration scripts located in `db/migration/` (`V1` to `V11`).
- Migrations define deterministic DDL: table creation, constraints, foreign keys, and indexes.
- Recent migration `V11__add_villa_inspections_workflow.sql` created the `villa_inspections` table with foreign keys referencing `rooms(id)` and `users(id)`, custom check constraints on statuses (`SCHEDULED`, `IN_PROGRESS`, `PASSED`, `ACTION_REQUIRED`), and indexes on `(room_id, status)` and `scheduled_at`.
- Flyway validates schema hashes (`flyway_schema_history`) upon Spring Boot bootstrap, ensuring environments stay in lockstep.

---

### Q3.2: What indexing strategies are used in the platform?
**Answer:**
- **B-Tree Indexes**: Placed on high-cardinality foreign keys (`booking.user_id`, `booking.room_id`, `review.room_id`, `villa_inspections.room_id`).
- **Composite Indexes**: On `(room_id, check_in_date, check_out_date, status)` to accelerate availability checks.
- **Partial Indexes**: On active vouchers (`is_active = true AND expiry_date > NOW()`) to keep index footprint minimal and lookups fast.

---

## 4. Security, Authentication & Role-Based Access Control (RBAC)

### Q4.1: Explain the JWT Authentication and RBAC flow.
**Answer:**
1. **Credentials Validation**: User sends credentials to `POST /api/auth/login`. Passwords are verified using `BCryptPasswordEncoder` (strength 12).
2. **Token Generation**: Spring Security generates a signed JWT (HMAC-SHA256) containing `userId`, `email`, and `roles` with standard claims (`sub`, `iat`, `exp`).
3. **Filter Chain**: `JwtAuthenticationFilter` intercepts incoming HTTP requests, extracts the Bearer token or HttpOnly cookie, validates signature and expiration, loads `UserDetails`, and populates `SecurityContextHolder.getContext().setAuthentication(...)`.
4. **Authorization Enforcement**: Method-level security (`@PreAuthorize("hasRole('ADMIN')")` or `hasAnyRole('INSPECTOR', 'ADMIN')`) and endpoint ant-matchers enforce fine-grained RBAC.

---

### Q4.2: How do you protect against CSRF and XSS attacks?
**Answer:**
- **Stateless REST**: Standard REST APIs use stateless JWT headers (`Authorization: Bearer`), rendering classic browser CSRF vector ineffective.
- **HttpOnly & Secure Cookies**: For cookie-based flows, tokens are stored in `HttpOnly; Secure; SameSite=Strict` cookies to prevent client-side JavaScript access.
- **XSS Prevention**: React automatically escapes strings in JSX rendering. Backend validates and sanitizes all freeform input strings using Jakarta Validation annotations (`@NotBlank`, `@Size`, `@Pattern`).

---

## 5. Payment Integration, Webhooks & Idempotency

### Q5.1: How is payment webhook idempotency guaranteed (e.g., VNPay IPN or Stripe Webhooks)?
**Answer:**
- **The Problem**: Payment gateways frequently retry IPN/Webhook HTTP callbacks if network latency delays the response, leading to risks of double-crediting or duplicate booking confirmations.
- **Our Solution**:
  1. **HMAC Signature Verification**: Compute SHA512 hash using the secret key and compare against `vnp_SecureHash`. Reject immediately if mismatched.
  2. **Idempotency Key & Transaction Guard**: The booking's `payment_status` is checked inside an atomic transaction. If `payment_status == PAID`, the system acknowledges `200 OK (RspCode: 00)` without executing downstream business logic again.
  3. **Atomic State Transition**: `PENDING -> CONFIRMED` transition is recorded with timestamp and gateway transaction ID.

---

## 6. Resilience, Caching & Message Queuing (RabbitMQ & Redis)

### Q6.1: What happens if Redis or RabbitMQ goes down during runtime?
**Answer:**
- **Cache Resilience (`CacheConfig.java`)**: Our Spring cache configuration detects Redis connection health. If Redis is unreachable, it logs a warning and automatically falls back to an in-memory `ConcurrentMapCacheManager`. System operation continues uninterrupted.
- **RabbitMQ Resilience**: Message producers use durable queues with dead-letter exchange (DLX) routing and retry policies. If RabbitMQ is temporarily offline, asynchronous events (e.g., email dispatch) gracefully fall back to local transactional outbox or sync fallback.

---

## 7. Inspector & Drone Aerial Survey Workflow

### Q7.1: What is the business purpose and technical implementation of the Drone Inspection feature?
**Answer:**
- **Business Need**: Luxury resorts require rigorous quality assurance before VIP check-ins (roof integrity, solar panel thermal hotspots, pool cleanliness, HVAC acoustic levels).
- **Technical Architecture**:
  - **Entity**: `VillaInspection` linked to `Room` and `User` (Inspector).
  - **Metrics Captured**: Drone model, battery consumption, flight altitude, thermal anomalies, structural score (0-100), pool clarity score, and HVAC acoustic decibels.
  - **Workflow**:
    1. Manager/Admin schedules inspection (`SCHEDULED`).
    2. Inspector launches drone mission, telemetry is captured (`IN_PROGRESS`).
    3. Multi-point checklist is evaluated.
    4. Inspector submits signed report (`PASSED` or `ACTION_REQUIRED`).
    5. If `ACTION_REQUIRED`, automated maintenance ticket is generated.
  - **Frontend**: `DroneInspectionPanel` features live telemetry cards, interactive visual checklist, and instant pass/fail action buttons.

---

## 8. Frontend Architecture (Next.js 15 App Router & React 19)

### Q8.1: Why Next.js 15 App Router and how is state managed?
**Answer:**
- **Hybrid Rendering**:
  - **SSR / Server Components**: Public villa catalog pages render on the server with optimal SEO meta tags and fast First Contentful Paint (FCP).
  - **Client Components (`"use client"`)**: Interactive modules like Admin Dashboard, Drone Mission Control, and Dynamic Room Customizer use React 19 hooks (`useState`, `useEffect`, `useCallback`, `useMemo`).
- **Unified API Client**: Centralized `client.ts` handles JWT injection, automatic token refresh, uniform error toast dispatching, and strongly typed endpoint mapping (`API_PATHS`).

---

## 9. DevOps, Docker, Health Checks & Observability

### Q9.1: How is the system monitored and containerized?
**Answer:**
- **Docker Compose**: Multi-container setup orchestrating `frontend`, `backend`, `postgres`, `redis`, and `rabbitmq`.
- **Spring Boot Actuator**: Exposes `/actuator/health` and `/actuator/metrics` for Kubernetes/Docker container liveness and readiness probes.
- **Structured Logging**: SLF4J / Logback with correlation IDs (`TraceId`, `SpanId`) in MDC for end-to-end request tracing.

---

## 10. Scenario-Based Behavioral & Live Defense Questions

### Q10.1: "A guest arrives but claims their booking was confirmed while the system shows PENDING because their VNPay callback failed. How does the system handle this?"
**Answer:**
1. Staff can access the Admin Booking panel and trigger a manual "Re-verify Gateway Status" or query VNPay query-DR API using the unique `bookingCode`.
2. Once the gateway confirms payment capture, staff can transition the booking to `CONFIRMED` and proceed to `CHECK_IN`.
3. An audit log entry is written recording the manual reconciliation.

### Q10.2: "What is your biggest engineering accomplishment in this project?"
**Answer:**
"Designing an end-to-end enterprise luxury resort platform that combines bulletproof transactional consistency (Pessimistic locking to prevent double bookings, idempotent webhook handling) with innovative operational features like the Drone Inspection & Telemetry workflow and a clean, responsive Next.js 15 frontend with zero entity leakage."
