package com.hsf.hotel.service;

import com.hsf.hotel.model.Promotion;
import com.hsf.hotel.model.Voucher;
import com.hsf.hotel.repository.PromotionRepository;
import com.hsf.hotel.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoucherService")
class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private PromotionRepository promotionRepository;

    @InjectMocks
    private VoucherService voucherService;

    private Voucher testVoucher;
    private Promotion testPromotion;

    @BeforeEach
    void setUp() {
        testVoucher = new Voucher();
        testVoucher.setId(1);
        testVoucher.setCode("TEST20");
        testVoucher.setAmount(BigDecimal.valueOf(20));
        testVoucher.setPercent(true);
        testVoucher.setQuantity(100);
        testVoucher.setExpiryDate(LocalDate.now().plusMonths(1));

        testPromotion = new Promotion();
        testPromotion.setId(1);
        testPromotion.setPromoCode("SUMMER25");
        testPromotion.setTitle("Summer Sale");
        testPromotion.setDescription("Summer promotion");
        testPromotion.setDiscountPercent(BigDecimal.valueOf(25));
        testPromotion.setMinimumNights(2);
        testPromotion.setMinimumBookingAmount(BigDecimal.valueOf(200));
        testPromotion.setIsActive(true);
        testPromotion.setStartDate(LocalDate.now().minusDays(10));
        testPromotion.setEndDate(LocalDate.now().plusDays(20));
    }

    @Nested
    class ValidationTests {
        @Test
        void validatesActiveVoucher() {
            when(voucherRepository.findByCode("TEST20")).thenReturn(Optional.of(testVoucher));

            var result = voucherService.validateVoucher("TEST20");

            assertTrue(result.valid);
            assertEquals(BigDecimal.valueOf(20), result.amount);
            assertTrue(result.percent);
            assertSame(testVoucher, result.voucher);
        }

        @Test
        void rejectsBlankCode() {
            var result = voucherService.validateVoucher("   ");

            assertFalse(result.valid);
            assertEquals(BigDecimal.ZERO, result.amount);
            verifyNoInteractions(voucherRepository, promotionRepository);
        }

        @Test
        void rejectsExpiredVoucher() {
            testVoucher.setExpiryDate(LocalDate.now().minusDays(1));
            when(voucherRepository.findByCode("EXPIRED")).thenReturn(Optional.of(testVoucher));

            var result = voucherService.validateVoucher("EXPIRED");

            assertFalse(result.valid);
        }

        @Test
        void rejectsExhaustedVoucher() {
            testVoucher.setQuantity(0);
            when(voucherRepository.findByCode("TEST20")).thenReturn(Optional.of(testVoucher));

            var result = voucherService.validateVoucher("TEST20");

            assertFalse(result.valid);
        }

        @Test
        void returnsInvalidForUnknownCode() {
            when(voucherRepository.findByCode("INVALID")).thenReturn(Optional.empty());
            when(promotionRepository.findActiveByPromoCode("INVALID")).thenReturn(Optional.empty());

            var result = voucherService.validateVoucher("INVALID");

            assertFalse(result.valid);
        }

        @Test
        void preservesTrimmedCodeForRepositoryLookup() {
            when(voucherRepository.findByCode("test20")).thenReturn(Optional.of(testVoucher));

            var result = voucherService.validateVoucher(" test20 ");

            assertTrue(result.valid);
            verify(voucherRepository).findByCode("test20");
        }
    }

    @Nested
    class PromotionValidationTests {
        @Test
        void validatesActivePromotionWhenVoucherDoesNotMatch() {
            when(voucherRepository.findByCode("SUMMER25")).thenReturn(Optional.empty());
            when(promotionRepository.findActiveByPromoCode("SUMMER25")).thenReturn(Optional.of(testPromotion));

            var result = voucherService.validateVoucher("SUMMER25");

            assertTrue(result.valid);
            assertEquals(BigDecimal.valueOf(25), result.amount);
            assertTrue(result.percent);
            assertSame(testPromotion, result.promotion);
        }

        @Test
        void rejectsInactivePromotion() {
            testPromotion.setIsActive(false);
            when(voucherRepository.findByCode("SUMMER25")).thenReturn(Optional.empty());
            when(promotionRepository.findActiveByPromoCode("SUMMER25")).thenReturn(Optional.of(testPromotion));

            var result = voucherService.validateVoucher("SUMMER25");

            assertFalse(result.valid);
        }

        @Test
        void rejectsPromotionOutsideDateWindow() {
            testPromotion.setEndDate(LocalDate.now().minusDays(1));
            when(voucherRepository.findByCode("SUMMER25")).thenReturn(Optional.empty());
            when(promotionRepository.findActiveByPromoCode("SUMMER25")).thenReturn(Optional.of(testPromotion));

            var result = voucherService.validateVoucher("SUMMER25");

            assertFalse(result.valid);
        }

        @Test
        void rejectsPromotionWithNoRemainingUses() {
            testPromotion.setMaximumUses(10);
            testPromotion.setCurrentUses(10);
            when(voucherRepository.findByCode("SUMMER25")).thenReturn(Optional.empty());
            when(promotionRepository.findActiveByPromoCode("SUMMER25")).thenReturn(Optional.of(testPromotion));

            var result = voucherService.validateVoucher("SUMMER25");

            assertFalse(result.valid);
        }
    }

    @Nested
    class ConsumptionTests {
        @Test
        void consumesVoucherAtomically() {
            when(voucherRepository.decrementQuantityAtomic(1)).thenReturn(1);

            boolean result = voucherService.consumeVoucher(testVoucher);

            assertTrue(result);
            assertEquals(99, testVoucher.getQuantity());
            verify(voucherRepository).decrementQuantityAtomic(1);
            verify(voucherRepository, never()).save(any(Voucher.class));
        }

        @Test
        void returnsFalseWhenAtomicConsumeDoesNotUpdate() {
            when(voucherRepository.decrementQuantityAtomic(1)).thenReturn(0);

            boolean result = voucherService.consumeVoucher(testVoucher);

            assertFalse(result);
            assertEquals(100, testVoucher.getQuantity());
        }

        @Test
        void returnsFalseForUnsavedVoucher() {
            testVoucher.setId(null);

            assertFalse(voucherService.consumeVoucher(testVoucher));
            verifyNoInteractions(voucherRepository);
        }
    }

    @Nested
    class ReleaseTests {
        @Test
        void releasesVoucherBackToPool() {
            when(voucherRepository.findByCodeIgnoreCase("TEST20")).thenReturn(Optional.of(testVoucher));
            when(voucherRepository.incrementQuantityAtomic(1)).thenReturn(1);

            voucherService.releaseVoucherByCode(" TEST20 ");

            assertEquals(101, testVoucher.getQuantity());
            verify(voucherRepository).findByCodeIgnoreCase("TEST20");
            verify(voucherRepository).incrementQuantityAtomic(1);
            verify(voucherRepository, never()).save(any(Voucher.class));
        }

        @Test
        void ignoresBlankReleaseCode() {
            voucherService.releaseVoucherByCode("");

            verifyNoInteractions(voucherRepository);
        }
    }
}
