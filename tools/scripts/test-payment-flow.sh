#!/bin/bash

# ============================================================
# Payment Flow Test Script
# Tests all payment scenarios
# ============================================================

set -e

API_BASE="${API_BASE:-http://localhost:8080/api/v1}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@hotel.com}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   Payment Flow Test Suite${NC}"
echo -e "${BLUE}========================================${NC}"

# Test helper function
test_api() {
    local name=$1
    local expected_status=$2
    shift 2
    local response=$(curl -s -w "\n%{http_code}" "$@")
    local status=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')
    
    if [ "$status" == "$expected_status" ]; then
        echo -e "${GREEN}✓${NC} $name (HTTP $status)"
        return 0
    else
        echo -e "${RED}✗${NC} $name - Expected $expected_status, got $status"
        echo "Response: $body"
        return 1
    fi
}

# Test 1: Health Check
echo -e "\n${YELLOW}1. Health Check${NC}"
test_api "Backend health" "200" "$API_BASE/health"

# Test 2: Create Test User
echo -e "\n${YELLOW}2. User Registration${NC}"
TEST_EMAIL="test_$(date +%s)@test.com"
TEST_RESPONSE=$(curl -s -X POST "$API_BASE/auth/register" \
    -H "Content-Type: application/json" \
    -d "{
        \"username\": \"testuser_$(date +%s)\",
        \"email\": \"$TEST_EMAIL\",
        \"password\": \"Test123!\",
        \"fullName\": \"Test User\"
    }")
echo "User created: $TEST_RESPONSE"

# Test 3: Login
echo -e "\n${YELLOW}3. User Login${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$API_BASE/auth/login" \
    -H "Content-Type: application/json" \
    -c cookies.txt \
    -d "{
        \"username\": \"$TEST_EMAIL\",
        \"password\": \"Test123!\"
    }")
echo "Login response: $LOGIN_RESPONSE"

# Test 4: Get Available Rooms
echo -e "\n${YELLOW}4. Room Availability${NC}"
ROOMS_RESPONSE=$(curl -s "$API_BASE/rooms")
ROOM_ID=$(echo "$ROOMS_RESPONSE" | jq -r '.[0].id // empty')
if [ -n "$ROOM_ID" ]; then
    echo -e "${GREEN}✓${NC} Found room ID: $ROOM_ID"
else
    echo -e "${YELLOW}⚠${NC} No rooms available"
fi

# Test 5: Create Booking
echo -e "\n${YELLOW}5. Create Booking${NC}"
if [ -n "$ROOM_ID" ]; then
    CHECKIN=$(date -d "+5 days" +%Y-%m-%d)
    CHECKOUT=$(date -d "+7 days" +%Y-%m-%d)
    
    BOOKING_RESPONSE=$(curl -s -X POST "$API_BASE/bookings" \
        -H "Content-Type: application/json" \
        -b cookies.txt \
        -d "{
            \"roomId\": $ROOM_ID,
            \"checkIn\": \"$CHECKIN\",
            \"checkOut\": \"$CHECKOUT\",
            \"guestName\": \"Test Guest\",
            \"guestEmail\": \"$TEST_EMAIL\",
            \"guestPhone\": \"0912345678\",
            \"guests\": 2
        }")
    
    BOOKING_ID=$(echo "$BOOKING_RESPONSE" | jq -r '.data.booking.id // empty')
    BOOKING_STATUS=$(echo "$BOOKING_RESPONSE" | jq -r '.data.booking.status // empty')
    
    if [ -n "$BOOKING_ID" ]; then
        echo -e "${GREEN}✓${NC} Booking created: ID=$BOOKING_ID, Status=$BOOKING_STATUS"
    else
        echo -e "${RED}✗${NC} Failed to create booking"
        echo "Response: $BOOKING_RESPONSE"
    fi
fi

# Test 6: Get Booking Details
echo -e "\n${YELLOW}6. Get Booking Details${NC}"
if [ -n "$BOOKING_ID" ]; then
    test_api "Get booking $BOOKING_ID" "200" "$API_BASE/bookings/$BOOKING_ID" -b cookies.txt
fi

# Test 7: Voucher Validation (Invalid)
echo -e "\n${YELLOW}7. Voucher Validation${NC}"
test_api "Validate invalid voucher" "200" "$API_BASE/vouchers/validate?code=INVALID123" || true

# Test 8: Admin Login
echo -e "\n${YELLOW}8. Admin Login${NC}"
ADMIN_RESPONSE=$(curl -s -X POST "$API_BASE/auth/login" \
    -H "Content-Type: application/json" \
    -c admin_cookies.txt \
    -d "{
        \"username\": \"admin\",
        \"password\": \"admin123\"
    }")
echo "Admin login response: $ADMIN_RESPONSE"

# Test 9: Admin Dashboard Access
echo -e "\n${YELLOW}9. Admin Dashboard${NC}"
test_api "Admin dashboard" "200" "$API_BASE/admin/dashboard" -b admin_cookies.txt || true

# Test 10: Payment Intent Creation (Mock Mode)
echo -e "\n${YELLOW}10. Payment Flow${NC}"
if [ -n "$BOOKING_ID" ]; then
    # Create payment intent
    PAYMENT_RESPONSE=$(curl -s -X POST "$API_BASE/payments/intent" \
        -H "Content-Type: application/json" \
        -b cookies.txt \
        -d "{
            \"bookingId\": $BOOKING_ID,
            \"amount\": 900000,
            \"method\": \"CARD\"
        }")
    
    INTENT_ID=$(echo "$PAYMENT_RESPONSE" | jq -r '.data.intentId // empty')
    PAYMENT_MODE=$(echo "$PAYMENT_RESPONSE" | jq -r '.data.mode // "mock"')
    
    echo -e "${BLUE}Payment Info:${NC}"
    echo "  Intent ID: $INTENT_ID"
    echo "  Mode: $PAYMENT_MODE"
    
    if [ "$PAYMENT_MODE" == "mock" ]; then
        echo -e "${YELLOW}⚠${NC} Running in mock mode - Stripe not configured"
    else
        echo -e "${GREEN}✓${NC} Real Stripe integration active"
    fi
fi

# Test 11: Stripe Checkout Session
echo -e "\n${YELLOW}11. Stripe Checkout Session${NC}"
if [ -n "$BOOKING_ID" ]; then
    CHECKOUT_RESPONSE=$(curl -s -X POST "$API_BASE/payments/stripe-checkout" \
        -H "Content-Type: application/json" \
        -b cookies.txt \
        -d "{
            \"bookingId\": $BOOKING_ID,
            \"successUrl\": \"http://localhost:3000/booking/success\",
            \"cancelUrl\": \"http://localhost:3000/checkout\"
        }")
    
    CHECKOUT_MODE=$(echo "$CHECKOUT_RESPONSE" | jq -r '.data.mode // "unknown"')
    CHECKOUT_URL=$(echo "$CHECKOUT_RESPONSE" | jq -r '.data.checkoutUrl // empty')
    
    echo "Checkout mode: $CHECKOUT_MODE"
    if [ -n "$CHECKOUT_URL" ]; then
        echo -e "${GREEN}✓${NC} Checkout URL: $CHECKOUT_URL"
    fi
fi

# Summary
echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}   Test Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo "API Base: $API_BASE"
echo "Test User: $TEST_EMAIL"
echo "Test Booking: $BOOKING_ID"
echo ""
echo -e "${YELLOW}Note:${NC} Some tests may fail if:"
echo "  - Backend is not running"
echo "  - Database is empty (no rooms)"
echo "  - Stripe is not configured (will use mock)"
echo ""

# Cleanup
rm -f cookies.txt admin_cookies.txt

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   Test Suite Complete${NC}"
echo -e "${BLUE}========================================${NC}"
