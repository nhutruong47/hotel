package com.hsf.hotel.service;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private ExcelExportService excelExportService;
    @Mock private PdfExportService pdfExportService;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(bookingRepository, excelExportService, pdfExportService);
    }

    @Test
    public void testGenerateBookingReport() throws IOException {
        when(bookingRepository.findAllByCheckInDateBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(new Booking()));
        when(excelExportService.exportBookingsToExcel(any())).thenReturn(new ByteArrayInputStream(new byte[0]));

        ByteArrayInputStream result = reportService.generateBookingReport(
                LocalDate.now(), LocalDate.now().plusDays(1));

        assertNotNull(result);
    }

    @Test
    public void testGeneratePdfBookingReport() throws IOException {
        when(bookingRepository.findAllByCheckInDateBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(new Booking()));
        when(pdfExportService.exportBookingsToPdf(any())).thenReturn(new ByteArrayInputStream(new byte[0]));

        ByteArrayInputStream result = reportService.generatePdfBookingReport(
                LocalDate.now(), LocalDate.now().plusDays(1));

        assertNotNull(result);
    }
}
