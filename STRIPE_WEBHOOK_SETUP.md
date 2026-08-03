# ============================================================
# STRIPE WEBHOOK SETUP GUIDE
# ============================================================

## Overview

Stripe Webhooks allow your backend to receive real-time notifications when payment events occur. This is essential for automatically updating booking statuses after successful payments.

## Step-by-Step Setup

### 1. Access Stripe Dashboard

1. Go to https://dashboard.stripe.com
2. Sign in to your account
3. Navigate to **Developers** → **Webhooks**

### 2. Add Webhook Endpoint

1. Click **"Add endpoint"** button
2. Enter your endpoint URL:
   ```
   https://yourdomain.com/api/v1/payments/webhook/stripe
   ```
   
   For local development with Stripe CLI:
   ```
   http://localhost:8080/api/v1/payments/webhook/stripe
   ```

3. Select the events to listen for:
   - [x] `checkout.session.completed`
   - [x] `payment_intent.succeeded`
   - [x] `payment_intent.payment_failed`
   - [x] `charge.refunded`

4. Click **"Add endpoint"**

### 3. Copy Webhook Signing Secret

After creating the endpoint, Stripe will show you:
- **Webhook URL**: Your endpoint URL
- **Signing secret**: `whsec_...`

Copy the signing secret and add it to your environment:

```bash
# .env or environment variable
STRIPE_WEBHOOK_SECRET=<STRIPE_WEBHOOK_SECRET>
```

### 4. Configure in Backend

Make sure your backend has the webhook secret configured:

```bash
export STRIPE_WEBHOOK_SECRET=<STRIPE_WEBHOOK_SECRET>
```

## Local Development with Stripe CLI

For local testing without deploying, use Stripe CLI:

### 1. Install Stripe CLI

```bash
# macOS
brew install stripe/stripe-cli/stripe

# Linux
sudo apt install stripe

# Windows
Download from: https://github.com/stripe/stripe-cli/releases
```

### 2. Login to Stripe CLI

```bash
stripe login
```

### 3. Forward Webhooks to Local Backend

```bash
stripe listen --forward-to localhost:8080/api/v1/payments/webhook/stripe
```

### 4. Copy the Webhook Signing Secret

The CLI will output a signing secret like:
```
Ready. Store the webhook signing secret in your deployment secrets manager.
```

Set this in your local `.env`:
```bash
STRIPE_WEBHOOK_SECRET=<STRIPE_WEBHOOK_SECRET>
```

### 5. Trigger Test Events

```bash
# Trigger a payment success event
stripe trigger checkout.session.completed

# Trigger a payment failure event
stripe trigger payment_intent.payment_failed

# Trigger a refund event
stripe trigger charge.refunded
```

## Testing Webhook Handlers

### Using Stripe CLI

```bash
# Watch webhook events
stripe listen --forward-to localhost:8080/api/v1/payments/webhook/stripe

# In another terminal, trigger events
stripe trigger checkout.session.completed
```

### Using curl

```bash
# Send a test webhook event
curl -X POST http://localhost:8080/api/v1/payments/webhook/stripe \
  -H "Content-Type: application/json" \
  -H "Stripe-Signature: whsec_test_secret" \
  -d '{
    "type": "checkout.session.completed",
    "data": {
      "object": {
        "id": "cs_test_xxx",
        "payment_intent": "pi_test_xxx",
        "metadata": {
          "booking_id": "123"
        }
      }
    }
  }'
```

## Webhook Events Handled

### 1. checkout.session.completed
Triggered when customer completes Stripe Checkout.

**Action**: Updates booking status to PAID, sends confirmation email.

```json
{
  "type": "checkout.session.completed",
  "data": {
    "object": {
      "id": "cs_xxx",
      "payment_intent": "pi_xxx",
      "metadata": {
        "booking_id": "123"
      }
    }
  }
}
```

### 2. payment_intent.succeeded
Triggered when a PaymentIntent succeeds (direct card payment).

**Action**: Updates booking status to PAID.

### 3. payment_intent.payment_failed
Triggered when a PaymentIntent fails.

**Action**: Logs failure, notifies admin if configured.

### 4. charge.refunded
Triggered when a charge is refunded.

**Action**: Updates payment status to REFUNDED.

## Security Considerations

### 1. Verify Webhook Signatures

The backend automatically verifies Stripe signatures using your `STRIPE_WEBHOOK_SECRET`. Never skip this verification in production.

### 2. Idempotency

Webhook handlers should be idempotent - the same event may be delivered multiple times. The backend uses database checks to prevent duplicate processing.

### 3. Error Handling

If your handler fails, Stripe will retry with exponential backoff:
- Retry 1: 5 minutes
- Retry 2: 30 minutes
- Retry 3: 2 hours
- Retry 4: 5 hours
- Retry 5: 10 hours
- Retry 6: 20 hours

### 4. Timeout

Stripe expects a response within **30 seconds**. Long operations (like sending emails) should be processed asynchronously.

## Troubleshooting

### Webhook Not Received

1. Check Stripe Dashboard → Webhooks → Your endpoint → "Failed webhook requests"
2. Verify your endpoint URL is publicly accessible
3. Check backend logs for errors
4. Ensure `STRIPE_WEBHOOK_SECRET` is correctly set

### Signature Verification Failed

1. Verify `STRIPE_WEBHOOK_SECRET` matches the dashboard
2. Check for any proxies modifying the request body
3. Ensure clock sync on your server (Stripe uses UTC)

### Events Not Processing

1. Check application logs for errors
2. Verify the webhook secret is being used
3. Test with Stripe CLI: `stripe logs tail`

### Duplicate Events

Stripe may send the same event multiple times. This is by design. The backend handles this by checking current booking state before updating.

## Quick Reference

| Event | Trigger | Action |
|-------|---------|--------|
| `checkout.session.completed` | Checkout finished | Update to PAID |
| `payment_intent.succeeded` | Direct payment success | Update to PAID |
| `payment_intent.payment_failed` | Payment failed | Log failure |
| `charge.refunded` | Refund processed | Update payment status |

## Environment Variables

```bash
# Required for webhooks
STRIPE_WEBHOOK_SECRET=whsec_xxx

# Also needed
STRIPE_SECRET_KEY=<STRIPE_SECRET_KEY>
STRIPE_ENABLED=true
```

## Testing Checklist

- [ ] Webhook endpoint is publicly accessible
- [ ] `STRIPE_WEBHOOK_SECRET` is configured
- [ ] Test `checkout.session.completed` event
- [ ] Test `payment_intent.payment_failed` event
- [ ] Verify booking status updates correctly
- [ ] Verify confirmation emails are sent
- [ ] Check application logs for errors
