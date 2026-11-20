package com.hsf.hotel.config;

import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * ADR-0009 Q6: the system currency is VND. Older data stored nightly rates on a
 * USD scale (450, 720…), which would be charged as 450 VND. This check never
 * changes data; it warns loudly at startup so an operator converts the prices.
 */
@Component
@Profile("!test")
public class CurrencySanityCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CurrencySanityCheck.class);
    static final BigDecimal MIN_PLAUSIBLE_NIGHTLY_VND = new BigDecimal("100000");

    private final RoomRepository roomRepository;

    public CurrencySanityCheck(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        try {
            List<String> suspicious = roomRepository.findAll().stream()
                    .filter(r -> r.getPricePerNight() != null
                            && r.getPricePerNight().compareTo(MIN_PLAUSIBLE_NIGHTLY_VND) < 0)
                    .map(r -> r.getRoomNumber() + "=" + r.getPricePerNight())
                    .toList();
            if (!suspicious.isEmpty()) {
                log.warn("CURRENCY CHECK: {} villa(s) have a nightly rate below {} VND and are probably still "
                                + "priced in USD: {}. Convert them to VND (see ADR-0009 Q6) — Stripe/SePay charge VND.",
                        suspicious.size(), MIN_PLAUSIBLE_NIGHTLY_VND, suspicious);
            }
        } catch (RuntimeException ex) {
            log.warn("CURRENCY CHECK skipped: {}", ex.getMessage());
        }
    }

    static boolean isSuspicious(Room room) {
        return room.getPricePerNight() != null && room.getPricePerNight().compareTo(MIN_PLAUSIBLE_NIGHTLY_VND) < 0;
    }
}
