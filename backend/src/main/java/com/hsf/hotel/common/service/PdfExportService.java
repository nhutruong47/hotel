package com.hsf.hotel.common.service;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.room.model.Room;

import com.hsf.hotel.booking.model.Booking;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PdfExportService {

    public ByteArrayInputStream exportBookingsToPdf(List<Booking> bookings) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.add(new Paragraph("Booking Report"));

            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 3, 2, 2, 2, 2, 2}));
            table.setWidth(UnitValue.createPercentValue(100));

            table.addHeaderCell("ID");
            table.addHeaderCell("User");
            table.addHeaderCell("Room");
            table.addHeaderCell("Check-in Date");
            table.addHeaderCell("Check-out Date");
            table.addHeaderCell("Status");
            table.addHeaderCell("Price");


            for (Booking booking : bookings) {
                table.addCell(String.valueOf(booking.getId()));
                table.addCell(booking.getUser().getFullName());
                table.addCell(booking.getRoom().getRoomNumber());
                table.addCell(booking.getCheckInDate().toString());
                table.addCell(booking.getCheckOutDate().toString());
                table.addCell(booking.getStatus().toString());
                table.addCell(booking.getTotalPrice().toString());
            }

            document.add(table);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}
