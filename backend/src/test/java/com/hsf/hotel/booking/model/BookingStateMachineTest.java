package com.hsf.hotel.booking.model;

import com.hsf.hotel.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingStateMachineTest {

    @Test
    void canonicalHappyPathIsAllowed() {
        assertTrue(BookingStateMachine.canTransition(BookingStatus.PENDING_PAYMENT, BookingStatus.PAID));
        assertTrue(BookingStateMachine.canTransition(BookingStatus.PAID, BookingStatus.CHECKED_IN));
        assertTrue(BookingStateMachine.canTransition(BookingStatus.CHECKED_IN, BookingStatus.CHECKED_OUT));
        assertTrue(BookingStateMachine.canTransition(BookingStatus.CHECKED_OUT, BookingStatus.COMPLETED));
    }

    @Test
    void backwardsAndSkippedTransitionsAreRejected() {
        assertFalse(BookingStateMachine.canTransition(BookingStatus.CHECKED_OUT, BookingStatus.PAID));
        assertFalse(BookingStateMachine.canTransition(BookingStatus.PAID, BookingStatus.COMPLETED));
        assertFalse(BookingStateMachine.canTransition(BookingStatus.CHECKED_IN, BookingStatus.CANCELLED));
        assertFalse(BookingStateMachine.canTransition(BookingStatus.COMPLETED, BookingStatus.CANCELLED));
    }

    @Test
    void pendingPaymentHasOnlyThreeValidTargets() {
        assertEquals(
                Set.of(BookingStatus.PAID, BookingStatus.CANCELLED, BookingStatus.EXPIRED),
                BookingStateMachine.allowedTargets(BookingStatus.PENDING_PAYMENT));
    }

    @Test
    void invalidTransitionRaisesStableBusinessError() {
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                BookingStateMachine.requireTransition(BookingStatus.CHECKED_OUT, BookingStatus.PAID));
        assertEquals("INVALID_BOOKING_TRANSITION", exception.getCode());
    }
}
