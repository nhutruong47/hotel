package com.hsf.hotel.booking.model;

import com.hsf.hotel.exception.BusinessRuleException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Single source of truth for the booking lifecycle. */
public final class BookingStateMachine {

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED = buildTransitions();

    private BookingStateMachine() {
    }

    public static boolean canTransition(BookingStatus from, BookingStatus to) {
        if (from == null || to == null || from == to) {
            return false;
        }
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static void requireTransition(BookingStatus from, BookingStatus to) {
        if (!canTransition(from, to)) {
            throw new BusinessRuleException(
                    "INVALID_BOOKING_TRANSITION",
                    "Không thể chuyển trạng thái đặt phòng từ " + from + " sang " + to);
        }
    }

    public static Set<BookingStatus> allowedTargets(BookingStatus from) {
        return Set.copyOf(ALLOWED.getOrDefault(from, Set.of()));
    }

    private static Map<BookingStatus, Set<BookingStatus>> buildTransitions() {
        EnumMap<BookingStatus, Set<BookingStatus>> transitions = new EnumMap<>(BookingStatus.class);
        transitions.put(BookingStatus.PENDING_PAYMENT,
                EnumSet.of(BookingStatus.PAID, BookingStatus.CANCELLED, BookingStatus.EXPIRED));
        transitions.put(BookingStatus.PAID,
                EnumSet.of(BookingStatus.CHECKED_IN, BookingStatus.CANCELLED, BookingStatus.NO_SHOW));
        transitions.put(BookingStatus.CHECKED_IN, EnumSet.of(BookingStatus.CHECKED_OUT));
        transitions.put(BookingStatus.CHECKED_OUT, EnumSet.of(BookingStatus.COMPLETED));
        return Map.copyOf(transitions);
    }
}
