package com.hsf.hotel.common.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** ADR-0009 Q6: invoices show whole VND, never a "$" amount. */
class InvoiceFormattingTest {

    @Test
    void formatsWholeVnd() {
        assertThat(InvoiceService.vnd(new BigDecimal("10692000"))).isEqualTo("10,692,000 VND");
        assertThat(InvoiceService.vnd(null)).isEqualTo("0 VND");
    }
}
