package com.hsf.hotel.service;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReportService {

    private final BookingRepository bookingRepository;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;

    public ReportService(BookingRepository bookingRepository,
                         ExcelExportService excelExportService,
                         PdfExportService pdfExportService) {
        this.bookingRepository = bookingRepository;
        this.excelExportService = excelExportService;
        this.pdfExportService = pdfExportService;
    }

    public ByteArrayInputStream generateBookingReport(LocalDate startDate, LocalDate endDate) throws IOException {
        return excelExportService.exportBookingsToExcel(
                bookingRepository.findAllByCheckInDateBetween(startDate, endDate));
    }

    public ByteArrayInputStream generatePdfBookingReport(LocalDate startDate, LocalDate endDate) throws IOException {
        return pdfExportService.exportBookingsToPdf(
                bookingRepository.findAllByCheckInDateBetween(startDate, endDate));
    }

    public ByteArrayInputStream generateBookingReportByRoom(Integer roomId) throws IOException {
        return excelExportService.exportBookingsToExcel(
                bookingRepository.findAllByRoomId(roomId));
    }

    public ByteArrayInputStream generatePdfBookingReportByRoom(Integer roomId) throws IOException {
        return pdfExportService.exportBookingsToPdf(
                bookingRepository.findAllByRoomId(roomId));
    }
}
