package com.hsf.hotel.service;

import com.hsf.hotel.model.Voucher;
import com.hsf.hotel.model.Promotion;
import com.hsf.hotel.repository.VoucherRepository;
import com.hsf.hotel.repository.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final PromotionRepository promotionRepository;

    public VoucherService(VoucherRepository voucherRepository, PromotionRepository promotionRepository) {
        this.voucherRepository = voucherRepository;
        this.promotionRepository = promotionRepository;
    }

    public static class VoucherValidationResult {
        public final boolean valid;
        public final String message;
        public final BigDecimal amount;
        public final boolean percent;
        public final Voucher voucher;
        public final Promotion promotion;

        public VoucherValidationResult(boolean valid, String message, BigDecimal amount,
                                       boolean percent, Voucher voucher) {
            this.valid = valid;
            this.message = message;
            this.amount = amount;
            this.percent = percent;
            this.voucher = voucher;
            this.promotion = null;
        }

        public VoucherValidationResult(boolean valid, String message, BigDecimal amount,
                                       boolean percent, Promotion promotion) {
            this.valid = valid;
            this.message = message;
            this.amount = amount;
            this.percent = percent;
            this.voucher = null;
            this.promotion = promotion;
        }
    }

    public VoucherValidationResult validateVoucher(String code) {
        if (code == null || code.trim().isEmpty()) {
            return new VoucherValidationResult(false, "Vui lòng nhập mã", BigDecimal.ZERO, false, (Voucher) null);
        }

        Optional<Voucher> opt = voucherRepository.findByCodeIgnoreCase(code.trim());
        if (opt.isPresent()) {
            Voucher v = opt.get();
            if (v.getExpiryDate() != null && v.getExpiryDate().isBefore(LocalDate.now())) {
                return new VoucherValidationResult(false, "Mã đã hết hạn", BigDecimal.ZERO, false, v);
            }
            if (v.getQuantity() == null || v.getQuantity() <= 0) {
                return new VoucherValidationResult(false, "Mã đã hết lượt sử dụng", BigDecimal.ZERO, false, v);
            }
            return new VoucherValidationResult(true, "Mã hợp lệ", v.getAmount(),
                    Boolean.TRUE.equals(v.getPercent()), v);
        }

        // Check active promotions if no voucher matches
        Optional<Promotion> promoOpt = promotionRepository.findByPromoCode(code.trim());
        if (promoOpt.isPresent()) {
            Promotion p = promoOpt.get();
            if (!p.isCurrentlyActive()) {
                return new VoucherValidationResult(false, "Ưu đãi đã hết hạn hoặc chưa bắt đầu", BigDecimal.ZERO, false, (Voucher) null);
            }
            if (p.getMaximumUses() != null && p.getCurrentUses() != null && p.getCurrentUses() >= p.getMaximumUses()) {
                return new VoucherValidationResult(false, "Ưu đãi đã hết lượt sử dụng", BigDecimal.ZERO, false, (Voucher) null);
            }
            BigDecimal amount = p.getDiscountPercent() != null ? p.getDiscountPercent() : p.getDiscountAmount();
            boolean isPercent = p.getDiscountPercent() != null;
            return new VoucherValidationResult(true, "Ưu đãi hợp lệ: " + p.getTitle(), amount, isPercent, p);
        }

        return new VoucherValidationResult(false, "Mã không tồn tại", BigDecimal.ZERO, false, (Voucher) null);
    }

    /**
     * Atomically decrement the voucher counter. Returns true if a unit was
     * consumed, false otherwise (voucher already exhausted or absent).
     * <p>
     * The repository runs an UPDATE ... WHERE quantity &gt; 0 at flush time
     * so concurrent callers cannot both pass the pre-transaction check and
     * decrement past zero. Must be invoked inside a transaction.
     */
    @Transactional
    public boolean consumeVoucher(Voucher voucher) {
        if (voucher == null || voucher.getId() == null) {
            return false;
        }
        int rows = voucherRepository.decrementQuantityAtomic(voucher.getId());
        if (rows == 0) {
            return false;
        }
        // Re-sync the in-memory entity so subsequent reads in the same
        // transaction see the new value.
        voucher.setQuantity(voucher.getQuantity() != null
                ? voucher.getQuantity() - 1
                : 0);
        return true;
    }

    /**
     * Return one voucher unit to the pool. Used when a booking is cancelled
     * or auto-expired before its voucher code was ever redeemed.
     */
    @Transactional
    public void releaseVoucherByCode(String code) {
        if (code == null || code.isBlank()) {
            return;
        }
        voucherRepository.findByCodeIgnoreCase(code.trim()).ifPresent(v -> {
            int rows = voucherRepository.incrementQuantityAtomic(v.getId());
            if (rows > 0) {
                v.setQuantity(v.getQuantity() != null ? v.getQuantity() + 1 : 1);
            }
        });
    }
}
