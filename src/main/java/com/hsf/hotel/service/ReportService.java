package com.hsf.hotel.service;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ExcelExportService excelExportService;

    @Autowired
    private PdfExportService pdfExportService;

    public ByteArrayInputStream generateBookingReport(LocalDate startDate, LocalDate endDate) throws IOException {
        List<Booking> bookings = bookingRepository.findAllByCheckInDateBetween(startDate, endDate);
        return excelExportService.exportBookingsToExcel(bookings);
    }

    public ByteArrayInputStream generatePdfBookingReport(LocalDate startDate, LocalDate endDate) throws IOException {
        List<Booking> bookings = bookingRepository.findAllByCheckInDateBetween(startDate, endDate);
        return pdfExportService.exportBookingsToPdf(bookings);
    }
}
