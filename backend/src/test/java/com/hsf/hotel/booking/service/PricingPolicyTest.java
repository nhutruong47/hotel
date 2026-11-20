package com.hsf.hotel.booking.service;

import com.hsf.hotel.room.model.Room;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** ADR-0009 Q1/Q6/Q7 pricing rules, all amounts VND. */
class PricingPolicyTest {

    private final PricingPolicy policy = PricingPolicy.defaults();

    private static Room room(long nightly, int capacity) {
        Room r = new Room();
        r.setPricePerNight(BigDecimal.valueOf(nightly));
        r.setCapacity(capacity);
        return r;
    }

    // 2026-10-05 is a Monday.
    private static final LocalDate MON = LocalDate.of(2026, 10, 5);

    @Test
    @DisplayName("Q7: fee 8% on discounted subtotal, VAT 10% on discounted subtotal + fee")
    void feeAndVatAfterDiscount() {
        PricingPolicy.Quote q = policy.quote(BigDecimal.valueOf(10_000_000), BigDecimal.valueOf(1_000_000));

        assertThat(q.discount()).isEqualByComparingTo("1000000");
        assertThat(q.serviceFee()).isEqualByComparingTo("720000");     // 8% x 9,000,000
        assertThat(q.taxAmount()).isEqualByComparingTo("972000");      // 10% x 9,720,000
        assertThat(q.total()).isEqualByComparingTo("10692000");        // 9,000,000 + 720,000 + 972,000
    }

    @Test
    @DisplayName("Discount is capped to the subtotal and never makes the total negative")
    void discountCapped() {
        PricingPolicy.Quote q = policy.quote(BigDecimal.valueOf(500_000), BigDecimal.valueOf(9_000_000));
        assertThat(q.discount()).isEqualByComparingTo("500000");
        assertThat(q.total()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Q6: extra guests from the 3rd guest at 625,000 VND per guest per night")
    void extraGuestFee() {
        BigDecimal subtotal = policy.subtotal(room(10_000_000, 6), MON, MON.plusDays(2), 4);
        // 2 weekday nights x 10,000,000 + 2 extra guests x 2 nights x 625,000
        assertThat(subtotal).isEqualByComparingTo("22500000");
    }

    @Test
    @DisplayName("Weekend nights (Fri, Sat) +15%")
    void weekendSurcharge() {
        LocalDate fri = MON.plusDays(4);
        BigDecimal subtotal = policy.subtotal(room(10_000_000, 4), fri, fri.plusDays(2), 2);
        assertThat(subtotal).isEqualByComparingTo("23000000");
    }

    @Test
    @DisplayName("Q1: day-use is 50% of one night")
    void dayUse() {
        assertThat(policy.subtotal(room(11_250_000, 4), MON, MON, 2)).isEqualByComparingTo("5625000");
    }

    @Test
    @DisplayName("Extra-guest fee is configurable")
    void configurableExtraGuestFee() {
        PricingPolicy custom = new PricingPolicy(BigDecimal.valueOf(500_000));
        BigDecimal subtotal = custom.subtotal(room(1_000_000, 4), MON, MON.plusDays(1), 3);
        assertThat(subtotal).isEqualByComparingTo("1500000");
    }
}
