# Nhu Villas — Luxury Resort Management & Direct Booking Platform

An enterprise-grade, direct-booking and operational management platform for luxury private villas. This repository contains two tightly integrated deliverables:

- **`frontend/`** — Next.js 15 (React 19, TypeScript, TailwindCSS, Lucide Icons) with Server-Side Rendering (SSR) for catalog discovery, responsive luxury aesthetics, dynamic room customizers, and an integrated Inspector / Drone Mission Control dashboard.
- **`backend/`** — Spring Boot 3.4+ (Java 21) REST API with domain-driven modular architecture, Flyway database migrations (`V1` to `V11`), pessimistic locking concurrency control, resilient fallback caching, and end-to-end sanitized DTOs.

> **Zero-Recompilation Architecture**: Uses 12-factor externalized configuration (`.env` / environment variables) enabling identical binaries to run locally against PostgreSQL 17, in containerized Docker networks, or in Kubernetes clusters.

---

## 1. System Architecture & High-Level Design

```
┌──────────────────────────────────────┐       JSON over REST / JWT       ┌─────────────────────────────────────────┐
│        Next.js 15 + React 19         │  ──────────────────────────────▶ │        Spring Boot 3.4 REST API         │
│  - SSR Catalog & SEO Optimization    │  ◀────────────────────────────── │  - Hexagonal Domain Architecture        │
│  - Dynamic Admin & Drone Panels      │       HttpOnly Cookie / Bearer   │  - Spring Security RBAC                 │
│  - Type-safe Client (`client.ts`)    │                                  │  - Pessimistic Concurrency Locking      │
└──────────────────────────────────────┘                                  └────────────────────┬────────────────────┘
                                                                                               │
                                                                   ┌───────────────────────────┴───────────────────────────┐
                                                                   ▼                                                       ▼
                                                     ┌───────────────────────────┐                           ┌───────────────────────────┐
                                                     │       PostgreSQL 17       │                           │       Redis / Fallback    │
                                                     │  Flyway Migrations (V1-11)│                           │  & RabbitMQ Async Outbox  │
                                                     │  Strict FKs & GiST Indexes│                           │  Resilient Event Bus      │
                                                     └───────────────────────────┘                           └───────────────────────────┘
```

| Domain Layer | Technology | Key Responsibilities |
| :--- | :--- | :--- |
| **Presentation** | Next.js 15 + React 19 + TailwindCSS | Luxury UI, interactive room builder, drone mission execution UI, admin CRUD. |
| **API Client** | TypeScript fetch wrapper (`client.ts`) | Unified error boundary, JWT interceptors, strongly typed endpoints (`API_PATHS`). |
| **Security & RBAC** | Spring Security 6 + JJWT | 5-tier role hierarchy (`CUSTOMER`, `STAFF`, `INSPECTOR`, `MANAGER`, `ADMIN`). |
| **Transactional Core** | Spring Data JPA + Hibernate 6 | Pessimistic locking (`SELECT FOR UPDATE`), zero DTO entity leakage. |
| **Database & Schema** | PostgreSQL 17 + Flyway | Versioned migrations (`V1` to `V11`), `villa_inspections`, strict constraints. |
| **Resilience & Messaging** | Redis + RabbitMQ | Caching with in-memory map fallback, durable notification outbox. |
| **Aerial Quality Survey** | Drone Telemetry Subsystem | Pre-arrival structural, thermal, acoustic, and pool clarity inspection logging. |

---

## 2. Core Functional Modules

### 2.1 Luxury Villa Catalog & Smart Booking
- **Pessimistic Write Locking**: Prevents double-booking race conditions during high-traffic intervals.
- **Rich Villa Metadata**: 15+ configurable attributes including high-resolution galleries, amenities selector, coordinates (Lat/Lng), surroundings, check-in/out policies, and minimum/maximum stay constraints.
- **Flexible Voucher Engine**: Real-time discount calculations with usage limits, expiry tracking, and concurrency-safe redemption counters.

### 2.2 Inspector & Drone Aerial Survey Workflow
- **Drone Mission Control**: Schedule pre-arrival aerial scans, track drone battery levels, altitude, thermal hotspot scans, structural integrity scores, and HVAC acoustic decibels.
- **Interactive Checklist**: Step-by-step inspector sign-off with automatic maintenance ticket dispatch upon defect detection.

### 2.3 Payments & Webhook Idempotency
- Multi-gateway support: **VNPay**, **Stripe**, **MoMo**, **PayPal**, and **Cash/On-Arrival**.
- Cryptographic HMAC signature verification and atomic database state guards to ensure 100% webhook callback idempotency.

### 2.4 AI Concierge & Smart Recommender
- Natural language chat assistant that analyzes user party size, vacation style (romantic, family, wellness), and dates to recommend matching luxury villas.

---

## 3. Repository Structure & Key Documents

```
hotel/
├── backend/                  # Java 21 Spring Boot Backend
│   ├── src/main/java/com/hsf/hotel/
│   │   ├── admin/            # Admin management APIs & DTO projections
│   │   ├── auth/             # JWT authentication, RBAC, user models
│   │   ├── booking/          # Concurrency-safe booking engine
│   │   ├── payment/          # Multi-gateway payment handlers & webhooks
│   │   ├── review/           # Verified reviews with zero entity leakage
│   │   ├── room/             # Rooms, amenities, & Drone Villa Inspections
│   │   └── config/           # Security, Flyway, Cache fallback, RabbitMQ
│   └── src/main/resources/db/postgresql/ # Flyway migrations (V1 to V11)
├── frontend/                 # Next.js 15 App Router Frontend
│   ├── src/app/              # Next.js route handlers and pages
│   ├── src/features/admin/   # Admin dashboards, Drone Inspection & Maintenance panels
│   ├── src/features/villas/  # Villa catalog, room details, customizers
│   └── src/shared/api/       # Unified API client & type declarations
└── docs/                     # Comprehensive Project Documentation
    ├── API_DOCUMENTATION.md          # Complete REST API specification
    ├── FINAL_PROJECT_INTERVIEW_QA.md # 360-degree technical defense & interview Q&A
    ├── ARCHITECTURE.md               # Deep-dive system architecture
    └── MASTER_PROJECT_PLAN.md        # Project delivery & roadmap milestones
```

---

## 4. Local Quickstart (PostgreSQL 17 Direct)

### 4.1 Prerequisites
- **Node.js**: `v20+` (or `v22 LTS`)
- **Java Development Kit**: `JDK 21` (configured in `JAVA_HOME`)
- **PostgreSQL**: `v17` running locally on port `5432` with database `hotel`

### 4.2 Database Setup
1. Create database in PostgreSQL:
   ```sql
   CREATE DATABASE hotel;
   ```
2. Flyway migrations (`V1` through `V11`) will automatically execute on application startup.

### 4.3 Running the Backend
```bash
cd backend
# Run with local PostgreSQL profile
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=postgres
# Backend running at: http://localhost:8080
```
Default bootstrap admin: `admin` / `admin@123` (or configured via `.env`).

### 4.4 Running the Frontend
```bash
cd frontend
npm install
npm run dev
# Frontend running at: http://localhost:3000 (or http://localhost:5173)
```

---

## 5. Testing & Quality Assurance

### 5.1 Backend Automated Tests
```bash
cd backend
./mvnw.cmd test
```
Executes comprehensive unit, integration, RBAC security, concurrency locking, and DTO projection tests.

### 5.2 Frontend Production Build
```bash
cd frontend
npm run build
```
Validates TypeScript type safety, Next.js page generation, and optimized client bundle assets.

---

## 6. Key Documentation Links

- 📖 [REST API Documentation](docs/API_DOCUMENTATION.md)
- 🎓 [Final Defense & Technical Interview Guide](docs/FINAL_PROJECT_INTERVIEW_QA.md)
- 🏗️ [Architectural Blueprint](docs/ARCHITECTURE.md)
- 📋 [Master Project Plan](docs/MASTER_PROJECT_PLAN.md)
