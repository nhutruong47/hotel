---
description: Complete booking workflow from user perspective
---

# Booking Workflow

## Canonical Status Flow

```text
PENDING_PAYMENT -> PAID -> CHECKED_IN -> CHECKED_OUT -> COMPLETED
       |            |
       |            +-> CANCELLED
       +-> CANCELLED | EXPIRED
                    +-> NO_SHOW
```

`COMPLETED`, `CANCELLED`, `EXPIRED`, and `NO_SHOW` are terminal states.
`PAID` can only be entered through a verified gateway webhook or an
authorised manual-payment operation.

## User Flow

1. Browse villas and choose dates.
2. Submit guest details; the backend re-checks availability while locking the
   villa row.
3. The booking is created as `PENDING_PAYMENT` and holds inventory until its
   server-provided expiry time (15 minutes by default).
4. Complete card or bank-transfer payment. The backend, never the browser,
   validates amount and transaction identity before moving the booking to
   `PAID`.
5. A user may cancel their own `PENDING_PAYMENT` or `PAID` booking subject to
   the cancellation/refund policy.
6. After checkout, the user may review the completed stay.

## Back-office Flow

- `STAFF`, `MANAGER`, and `ADMIN` may view operational booking queues.
- `STAFF` or higher may check in, check out, and mark a paid booking no-show.
- `MANAGER` or higher manages rooms, promotions, reports, and maintenance.
- Only `ADMIN` manages roles, refunds, audit access, and administrative
  cancellation.
- Manual refund requests require a stable `Idempotency-Key`; retry the same
  operation with the same key.
- Only the Stripe and SePay provider-specific webhook routes may settle a
  payment. Do not restore a generic client-supplied payment-status webhook.

## Scheduled Jobs

- Every five minutes, expire `PENDING_PAYMENT` bookings whose hold has ended.
- Hourly, complete checked-out bookings after their checkout date.
- Daily, mark overdue paid bookings with no check-in as `NO_SHOW`.

## Verification

- Test boundary-touching date ranges and maintenance overlaps.
- Test two concurrent booking attempts for the same villa/date range.
- Test duplicate and late payment webhooks.
- Gateway webhook handlers must return non-2xx when reconciliation fails so
  the provider retries the event.
- Test refund retries and gateway rejection without changing local totals.
- Email events use the transactional `notification_outbox`: create the event
  inside the business transaction, publish only committed rows, and retain
  failed rows for retry. Rabbit publisher confirmation is required before a
  row becomes `PUBLISHED`.
- Test every back-office endpoint against USER, STAFF, MANAGER, and ADMIN.
