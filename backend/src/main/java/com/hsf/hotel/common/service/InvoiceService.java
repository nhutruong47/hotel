package com.hsf.hotel.common.service;
import com.hsf.hotel.room.model.Room;

import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.payment.model.Payment;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.property.TextAlignment;
import org.springframework.stereotype.Service;
import com.hsf.hotel.exception.FileStorageException;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InvoiceService {

    public byte[] generateInvoice(Booking booking, List<Payment> payments) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Header
            Paragraph header = new Paragraph("INVOICE")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(20);
            document.add(header);

            // Booking Info
            document.add(new Paragraph("Booking Reference: #" + booking.getId()));
            document.add(new Paragraph("Guest Name: " + booking.getGuestName()));
            document.add(new Paragraph("Check-in: " + booking.getCheckInDate()));
            document.add(new Paragraph("Check-out: " + booking.getCheckOutDate()));
            document.add(new Paragraph("Room: " + booking.getRoom().getRoomNumber()));
            document.add(new Paragraph("\n"));

            // Payments Table
            Table table = new Table(new float[]{100F, 100F, 100F, 100F});
            table.addHeaderCell(new Cell().add(new Paragraph("Payment ID").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Status").setBold()));

            for (Payment payment : payments) {
                table.addCell(new Cell().add(new Paragraph(String.valueOf(payment.getId()))));
                table.addCell(new Cell().add(new Paragraph(payment.getCreatedAt() != null ? payment.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE) : "")));
                table.addCell(new Cell().add(new Paragraph(vnd(payment.getAmount()))));
                table.addCell(new Cell().add(new Paragraph(payment.getStatus().name())));
            }

            document.add(table);

            // ADR-0009 Q6/Q7: amounts are VND; totalPrice already includes the discount,
            // service fee and VAT (it was previously reduced by the discount a second time).
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Subtotal: " + vnd(booking.getSubtotalPrice())));
            document.add(new Paragraph("Discount: -" + vnd(booking.getDiscountAmount())));
            document.add(new Paragraph("Service fee (8%): " + vnd(booking.getServiceFee())));
            document.add(new Paragraph("VAT (10%): " + vnd(booking.getTaxAmount())));
            document.add(new Paragraph("Total: " + vnd(booking.getTotalPrice())).setBold());
            if (booking.getRefundAmount() != null && booking.getRefundAmount().signum() > 0) {
                document.add(new Paragraph("Refunded: " + vnd(booking.getRefundAmount())));
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new FileStorageException("Error generating invoice PDF", e);
        }
    }

    /** "1,234,000 VND" — whole VND, matching the DECIMAL(12,0) money columns. */
    static String vnd(java.math.BigDecimal amount) {
        java.math.BigDecimal value = amount != null ? amount : java.math.BigDecimal.ZERO;
        return String.format(java.util.Locale.US, "%,.0f VND", value);
    }
}
