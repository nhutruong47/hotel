package com.hsf.hotel.service;

import com.hsf.hotel.model.Voucher;
import com.hsf.hotel.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class VoucherService {

    @Autowired
    private VoucherRepository voucherRepository;

    public static class VoucherValidationResult {
        public boolean valid;
        public String message;
        public BigDecimal amount;
        public Voucher voucher;

        public VoucherValidationResult(boolean valid, String message, BigDecimal amount, Voucher voucher) {
            this.valid = valid;
            this.message = message;
            this.amount = amount;
            this.voucher = voucher;
        }
    }

    public VoucherValidationResult validateVoucher(String code) {
        if (code == null || code.trim().isEmpty()) {
            return new VoucherValidationResult(false, "Please enter code", BigDecimal.ZERO, null);
        }

        Optional<Voucher> opt = voucherRepository.findByCodeIgnoreCase(code.trim());
        if (opt.isEmpty()) {
            return new VoucherValidationResult(false, "Code does not exist", BigDecimal.ZERO, null);
        }

        Voucher v = opt.get();

        if (v.getExpiryDate() != null && v.getExpiryDate().isBefore(LocalDate.now())) {
            return new VoucherValidationResult(false, "Code has expired", BigDecimal.ZERO, v);
        }

        if (v.getQuantity() == null || v.getQuantity() <= 0) {
            return new VoucherValidationResult(false, "Code is out of uses", BigDecimal.ZERO, v);
        }

        return new VoucherValidationResult(true, "Valid code", v.getAmount(), v);
    }

    @Transactional
    public void consumeVoucher(Voucher voucher) {
        if (voucher == null)
            return;
        if (voucher.getQuantity() == null || voucher.getQuantity() <= 0)
            return;
        voucher.setQuantity(voucher.getQuantity() - 1);
        voucherRepository.save(voucher);
    }
}
