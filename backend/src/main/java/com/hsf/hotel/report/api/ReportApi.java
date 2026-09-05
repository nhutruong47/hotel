package com.hsf.hotel.report.api;

import com.hsf.hotel.report.dto.ReportDTO;
import com.hsf.hotel.report.service.ReportService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1/admin/reports")
public class ReportApi {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReportService reportService;

    public ReportApi(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/excel")
    public ResponseEntity<InputStreamResource> excel(@ModelAttribute ReportDTO reportDTO) throws IOException {
        ByteArrayInputStream in = reportService.generateBookingReport(reportDTO.getStartDate(), reportDTO.getEndDate());
        return attachment("bookings.xlsx", XLSX_CONTENT_TYPE, in);
    }

    @PostMapping("/pdf")
    public ResponseEntity<InputStreamResource> pdf(@ModelAttribute ReportDTO reportDTO) throws IOException {
        ByteArrayInputStream in = reportService.generatePdfBookingReport(reportDTO.getStartDate(), reportDTO.getEndDate());
        return attachment("bookings.pdf", MediaType.APPLICATION_PDF_VALUE, in);
    }

    @PostMapping("/excel/room")
    public ResponseEntity<InputStreamResource> excelByRoom(@ModelAttribute ReportDTO reportDTO) throws IOException {
        ByteArrayInputStream in = reportService.generateBookingReportByRoom(reportDTO.getRoomId());
        return attachment("bookings_room_" + reportDTO.getRoomId() + ".xlsx", XLSX_CONTENT_TYPE, in);
    }

    @PostMapping("/pdf/room")
    public ResponseEntity<InputStreamResource> pdfByRoom(@ModelAttribute ReportDTO reportDTO) throws IOException {
        ByteArrayInputStream in = reportService.generatePdfBookingReportByRoom(reportDTO.getRoomId());
        return attachment("bookings_room_" + reportDTO.getRoomId() + ".pdf", MediaType.APPLICATION_PDF_VALUE, in);
    }

    private static ResponseEntity<InputStreamResource> attachment(String filename, String contentType,
                                                                 ByteArrayInputStream body) {
        HttpHeaders h = new HttpHeaders();
        h.add("Content-Disposition", "attachment; filename=" + filename);
        return ResponseEntity.ok().headers(h)
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(body));
    }
}
