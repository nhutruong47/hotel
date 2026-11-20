package com.hsf.hotel.booking.service;

import com.hsf.hotel.room.model.Room;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Single source of truth for booking prices (ADR-0009). All amounts are VND
 * with no minor unit, matching the {@code DECIMAL(12,0)} money columns.
 *
 * <ul>
 *   <li>Overnight: sum of nightly rates, Friday/Saturday nights +15%.</li>
 *   <li>Day-use (check-in == check-out): 50% of one nightly rate (Q1).</li>
 *   <li>Extra guests: from the 3rd guest, {@code app.pricing.extra-guest-fee-per-night}
 *       per guest per night (Q6, default 625,000 VND).</li>
 *   <li>Discount is capped to the subtotal. Service fee 8% is charged on the
 *       discounted subtotal; VAT 10% on discounted subtotal + service fee (Q7:
 *       Circular 219/2013/TT-BTC art. 7 and the "++" industry convention).</li>
 * </ul>
 */
@Component
public class PricingPolicy {

    public static final BigDecimal DEFAULT_EXTRA_GUEST_FEE_PER_NIGHT = new BigDecimal("625000");
    static final BigDecimal DAY_USE_FACTOR = new BigDecimal("0.5");
    static final BigDecimal WEEKEND_FACTOR = new BigDecimal("1.15");
    static final BigDecimal SERVICE_FEE_RATE = new BigDecimal("0.08");
    static final BigDecimal VAT_RATE = new BigDecimal("0.10");
    static final int BASE_GUESTS = 2;

    private final BigDecimal extraGuestFeePerNight;

    public PricingPolicy(
            @Value("${app.pricing.extra-guest-fee-per-night:625000}") BigDecimal extraGuestFeePerNight) {
        this.extraGuestFeePerNight = extraGuestFeePerNight != null && extraGuestFeePerNight.signum() >= 0
                ? extraGuestFeePerNight : DEFAULT_EXTRA_GUEST_FEE_PER_NIGHT;
    }

    /** Policy with default configuration — for code paths and tests without Spring. */
    public static PricingPolicy defaults() {
        return new PricingPolicy(DEFAULT_EXTRA_GUEST_FEE_PER_NIGHT);
    }

    public record Quote(BigDecimal subtotal, BigDecimal discount, BigDecimal serviceFee,
                        BigDecimal taxAmount, BigDecimal total) {}

    /** Accommodation subtotal (before discount, fee and VAT). */
    public BigDecimal subtotal(Room room, LocalDate checkIn, LocalDate checkOut, Integer guests) {
        BigDecimal rate = room.getPricePerNight() != null ? room.getPricePerNight() : BigDecimal.ZERO;
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) {
            return rate.multiply(DAY_USE_FACTOR).setScale(0, RoundingMode.HALF_UP);
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            DayOfWeek dow = night.getDayOfWeek();
            BigDecimal nightly = (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY)
                    ? rate.multiply(WEEKEND_FACTOR).setScale(0, RoundingMode.HALF_UP)
                    : rate;
            subtotal = subtotal.add(nightly);
        }
        if (guests != null && guests > BASE_GUESTS
                && room.getCapacity() != null && room.getCapacity() > BASE_GUESTS) {
            subtotal = subtotal.add(extraGuestFeePerNight
                    .multiply(BigDecimal.valueOf(guests - BASE_GUESTS))
                    .multiply(BigDecimal.valueOf(nights)));
        }
        return subtotal;
    }

    /** Fee, VAT and total for a subtotal and a (possibly zero) discount. */
    public Quote quote(BigDecimal subtotal, BigDecimal discount) {
        BigDecimal sub = subtotal != null ? subtotal : BigDecimal.ZERO;
        BigDecimal disc = discount == null || discount.signum() < 0 ? BigDecimal.ZERO : discount;
        if (disc.compareTo(sub) > 0) {
            disc = sub;
        }
        disc = disc.setScale(0, RoundingMode.HALF_UP);
        BigDecimal base = sub.subtract(disc);
        BigDecimal serviceFee = base.multiply(SERVICE_FEE_RATE).setScale(0, RoundingMode.HALF_UP);
        BigDecimal vat = base.add(serviceFee).multiply(VAT_RATE).setScale(0, RoundingMode.HALF_UP);
        return new Quote(sub, disc, serviceFee, vat, base.add(serviceFee).add(vat));
    }

    public BigDecimal extraGuestFeePerNight() {
        return extraGuestFeePerNight;
    }
}
