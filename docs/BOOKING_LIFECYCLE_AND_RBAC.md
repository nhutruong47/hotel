# Booking Lifecycle and RBAC

Status: Accepted
Last updated: 2026-09-11

This document is the business source of truth for booking status and
back-office permissions. Older documents that describe an approval-first
`PENDING -> AWAITING_PAYMENT -> CONFIRMED` workflow are historical.

## Booking lifecycle

```text
PENDING_PAYMENT -> PAID -> CHECKED_IN -> CHECKED_OUT -> COMPLETED
       |            |
       |            +-> CANCELLED
       +-> CANCELLED | EXPIRED
                    +-> NO_SHOW
```

Allowed transitions:

| From | To |
| --- | --- |
| `PENDING_PAYMENT` | `PAID`, `CANCELLED`, `EXPIRED` |
| `PAID` | `CHECKED_IN`, `CANCELLED`, `NO_SHOW` |
| `CHECKED_IN` | `CHECKED_OUT` |
| `CHECKED_OUT` | `COMPLETED` |

`COMPLETED`, `CANCELLED`, `EXPIRED`, and `NO_SHOW` are terminal. Payment is
the only valid way to enter `PAID`; a generic status update must never do so.

## Inventory hold

`holdExpiresAt` is the authoritative expiry for a `PENDING_PAYMENT` booking.
The default is 15 minutes and is configured with
`APP_BOOKING_HOLD_MINUTES`. `paymentDeadline` is temporarily retained as a
compatibility column and must contain the same instant.

Once the hold expires, the scheduler changes the booking to `EXPIRED`,
returns any consumed voucher unit, and releases the villa dates. A late
payment must be reconciled manually and must not resurrect an expired
booking.

## Payment and refund integrity

- A browser may register a `PENDING` payment attempt, but it cannot move a
  booking to `PAID`.
- Payment amount must exactly match the server-calculated booking total.
- Webhook processing locks the payment row; duplicate successful events are
  idempotent, and a success arriving after a failure may still settle.
- Stripe Checkout payments are correlated to the actual Checkout Session and
  discovered PaymentIntent. Reconciliation failures return non-2xx so Stripe
  can retry instead of silently losing the event.
- Stripe Session status lookups first resolve an internal payment and enforce
  booking ownership; possession of a Stripe session ID or mutable provider
  metadata is not authorization.
- SePay requires an exact API-key match and a stable transaction ID. A late
  transfer is recorded for reconciliation without reviving released inventory.
- There is no generic webhook that accepts a caller-supplied payment status.
  Only the Stripe signature-verified and SePay API-key-authenticated routes are
  publicly reachable.
- Refund endpoints require an `Idempotency-Key`. Each attempt is persisted in
  `payment_refunds`; repeating a successful key returns the prior result, and
  a gateway rejection never increments local refund totals.

## Notification consistency

Email events are stored in `notification_outbox` in the same transaction as
the booking, payment, or account mutation. This guarantees that rolled-back
business changes do not produce emails and committed changes survive a
temporary RabbitMQ outage. The dispatcher locks a small ready batch, retries
failures with bounded exponential backoff, and marks a row `PUBLISHED` only
after RabbitMQ publisher confirmation. Serialized payloads, including short-
lived verification/reset tokens, are cleared after successful publication;
published metadata is purged after the configured retention period.

Delivery is at-least-once: a process crash after broker confirmation but before
the database commit can produce a duplicate email. Consumers and email content
must therefore remain safe when repeated.

User-facing notification mutations include ownership in the database update
or delete predicate (`notification_id + user_id`). Missing notifications and
notifications owned by another account both return `404`, avoiding an ID
enumeration side channel. Notification list requests accept page sizes from 1
through 100 only. List responses use an explicit DTO and never expose the
owning `User` entity.

## Wishlist consistency

Wishlist mutations are serialized with a per-user database lock and the
database unique constraint on `(user_id, room_id)` remains the final invariant.
New clients should use the idempotent routes:

- `PUT /api/v1/wishlist/{roomId}` leaves the room present.
- `DELETE /api/v1/wishlist/{roomId}` leaves the room absent.

`POST /api/v1/wishlist/toggle` remains available for compatibility, but it is
non-idempotent by nature and should not be used for retried requests. Wishlist
list responses contain only the item ID, creation time, and a room summary;
the owning `User` entity is not part of the public contract.

## Account and preference consistency

Account emails are normalized with `trim + lowercase` before every write or
lookup. Migration V9 applies the same rule to existing rows and creates a
PostgreSQL unique index; it intentionally stops if legacy case variants would
collapse to one address, so account ownership must be resolved explicitly.

Registration does not create an authenticated session until email verification
has completed. The session bridge also invalidates any legacy session whose
account is disabled or unverified, including a session made stale by an email
change.

Preference updates are partial and serialized under a user-row lock. `PATCH
/api/v1/profile/preferences` is the preferred route; `PUT` remains supported
for compatibility. Unspecified fields are preserved and enum-like values are
validated at both API and service boundaries.

PostgreSQL migration V10 removes the obsolete `users.password_hash` column
after copying it into the entity-backed `users.password` column when needed.
Production rollout must back up the users table and run the V9 duplicate-email
preflight before applying V9/V10.

## Role hierarchy

```text
ADMIN > MANAGER > STAFF > USER
```

| Capability | USER | STAFF | MANAGER | ADMIN |
| --- | :---: | :---: | :---: | :---: |
| Own booking/profile/payment | Yes | Yes | Yes | Yes |
| Operational booking queues | No | Yes | Yes | Yes |
| Check-in, check-out, no-show | No | Yes | Yes | Yes |
| Contact/support operations | No | Yes | Yes | Yes |
| Rooms, catalog, promotions, maintenance | No | No | Yes | Yes |
| Reviews and analytics | No | No | Yes | Yes |
| Administrative cancellation/refund | No | No | No | Yes |
| User roles and security audit | No | No | No | Yes |

Backend authorization is authoritative. Frontend visibility is only a user
experience aid and must not replace endpoint authorization.

## Migration policy

Legacy statuses are migrated as follows:

| Legacy | Canonical |
| --- | --- |
| `AWAITING_APPROVAL` | `PENDING_PAYMENT` |
| `CONFIRMED` | `PAID` |
| `REJECTED` | `CANCELLED` |

Never edit an already deployed Flyway migration. Schema changes must be added
as a new forward-only migration and validated against a clean PostgreSQL
database before release.
