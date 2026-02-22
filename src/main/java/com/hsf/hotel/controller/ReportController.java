package com.hsf.hotel.controller;

import com.hsf.hotel.dto.ReportDTO;
import com.hsf.hotel.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Controller
@RequestMapping("/admin/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping("/excel")
    public ResponseEntity<InputStreamResource> getBookingReportExcel(@ModelAttribute ReportDTO reportDTO)
            throws IOException {
        ByteArrayInputStream in = reportService.generateBookingReport(reportDTO.getStartDate(), reportDTO.getEndDate());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=bookings.xlsx");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    @PostMapping("/pdf")
    public ResponseEntity<InputStreamResource> getBookingReportPdf(@ModelAttribute ReportDTO reportDTO)
            throws IOException {
        ByteArrayInputStream in = reportService.generatePdfBookingReport(reportDTO.getStartDate(),
                reportDTO.getEndDate());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=bookings.pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(in));
    }

    @PostMapping("/excel/room")
    public ResponseEntity<InputStreamResource> getBookingReportExcelByRoom(@ModelAttribute ReportDTO reportDTO)
            throws IOException {
        ByteArrayInputStream in = reportService.generateBookingReportByRoom(reportDTO.getRoomId());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=bookings_room_" + reportDTO.getRoomId() + ".xlsx");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    @PostMapping("/pdf/room")
    public ResponseEntity<InputStreamResource> getBookingReportPdfByRoom(@ModelAttribute ReportDTO reportDTO)
            throws IOException {
        ByteArrayInputStream in = reportService.generatePdfBookingReportByRoom(reportDTO.getRoomId());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=bookings_room_" + reportDTO.getRoomId() + ".pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(in));
    }
}
