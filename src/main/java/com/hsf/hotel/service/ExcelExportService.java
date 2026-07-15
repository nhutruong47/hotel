package com.hsf.hotel.service;

import com.hsf.hotel.model.Booking;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    public ByteArrayInputStream exportBookingsToExcel(List<Booking> bookings) throws IOException {
        String[] columns = {"ID", "User", "Room", "Check-in Date", "Check-out Date", "Status", "Price" };
        try (
                XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
        ) {
            XSSFSheet sheet = workbook.createSheet("Bookings");

            Row headerRow = sheet.createRow(0);

            for (int col = 0; col < columns.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(columns[col]);
            }

            int rowIdx = 1;
            for (Booking booking : bookings) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(booking.getId());
                row.createCell(1).setCellValue(booking.getUser().getFullName());
                row.createCell(2).setCellValue(booking.getRoom().getRoomNumber());
                row.createCell(3).setCellValue(booking.getCheckInDate().toString());
                row.createCell(4).setCellValue(booking.getCheckOutDate().toString());
                row.createCell(5).setCellValue(booking.getStatus().toString());
                row.createCell(6).setCellValue(booking.getTotalPrice().doubleValue());
            }
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
