package com.hsf.hotel.controller;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.Review;
import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.User;
import com.hsf.hotel.repository.BookingRepository;
import com.hsf.hotel.service.BookingService;
import com.hsf.hotel.service.ReviewService;
import com.hsf.hotel.service.RoomService;
import com.hsf.hotel.service.VoucherService;
import java.math.BigDecimal;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private VoucherService voucherService;

    @GetMapping("/booking/{roomId}")
    public String bookingForm(@PathVariable Integer roomId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        return roomService.getRoomById(roomId)
                .map(room -> {
                    model.addAttribute("room", room);
                    model.addAttribute("user", user);
                    model.addAttribute("minDate", LocalDate.now());

                    // Review data
                    Double avgRating = reviewService.getAverageRating(room);
                    long reviewCount = reviewService.getReviewCount(room);
                    List<Review> recentReviews = reviewService.getReviewsByRoom(room);
                    if (recentReviews.size() > 3) {
                        recentReviews = recentReviews.subList(0, 3);
                    }
                    model.addAttribute("avgRating", avgRating);
                    model.addAttribute("reviewCount", reviewCount);
                    model.addAttribute("recentReviews", recentReviews);

                    return "booking";
                })
                .orElse("redirect:/");
    }

    // API: Get booked dates for a room (used by JavaScript on booking page)
    @GetMapping("/api/rooms/{roomId}/booked-dates")
    @ResponseBody
    public ResponseEntity<List<Map<String, String>>> getBookedDates(@PathVariable Integer roomId) {
        List<Booking> activeBookings = bookingRepository.findActiveBookingsByRoomId(roomId);
        List<Map<String, String>> bookedRanges = activeBookings.stream().map(b -> {
            Map<String, String> range = new HashMap<>();
            range.put("checkIn", b.getCheckInDate().toString());
            range.put("checkOut", b.getCheckOutDate().toString());
            range.put("status", b.getStatus().name());
            return range;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(bookedRanges);
    }

    // AJAX endpoint to validate voucher codes
    @ResponseBody
    @PostMapping("/api/vouchers/validate")
    public java.util.Map<String, Object> validateVoucher(@RequestParam String code) {
        VoucherService.VoucherValidationResult res = voucherService.validateVoucher(code);
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("valid", res.valid);
        map.put("message", res.message);
        map.put("amount", res.amount != null ? res.amount.toString() : "0");
        map.put("percent", res.voucher != null && res.voucher.getPercent() != null ? res.voucher.getPercent() : false);
        return map;
    }

    @PostMapping("/booking")
    public String createBooking(@RequestParam Integer roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam String guestName,
            @RequestParam(required = false) String guestPhone,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String voucherCode,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Validate dates
        if (checkIn.isBefore(LocalDate.now())) {
            redirectAttributes.addFlashAttribute("error", "Ngày check-in không được trước hôm nay");
            return "redirect:/booking/" + roomId;
        }
        if (checkOut.isBefore(checkIn)) {
            redirectAttributes.addFlashAttribute("error", "Ngày trả phòng phải bằng hoặc sau ngày nhận phòng");
            return "redirect:/booking/" + roomId;
        }

        try {
            Room room = roomService.getRoomById(roomId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

            Booking booking = bookingService.createBooking(user, room, checkIn, checkOut, guestName, guestPhone, notes);

            // If voucherCode provided and valid, validate and apply discount to booking
            if (voucherCode != null && !voucherCode.trim().isEmpty()) {
                VoucherService.VoucherValidationResult vres = voucherService.validateVoucher(voucherCode);
                if (vres.valid && vres.voucher != null) {
                    BigDecimal discount = BigDecimal.ZERO;
                    if (vres.voucher.getPercent() != null && vres.voucher.getPercent()) {
                        // percent stored in vres.amount (e.g., 10 for 10%)
                        discount = booking.getTotalPrice().multiply(vres.amount).divide(BigDecimal.valueOf(100));
                    } else {
                        discount = vres.amount != null ? vres.amount : BigDecimal.ZERO;
                    }
                    BigDecimal newTotal = booking.getTotalPrice().subtract(discount);
                    if (newTotal.compareTo(BigDecimal.ZERO) < 0)
                        newTotal = BigDecimal.ZERO;

                    booking.setTotalPrice(newTotal);
                    booking.setAppliedVoucherCode(vres.voucher.getCode());
                    booking.setDiscountAmount(discount);

                    // Persist booking changes
                    bookingService.saveBooking(booking);

                    // consume voucher (decrement quantity)
                    voucherService.consumeVoucher(vres.voucher);

                    redirectAttributes.addFlashAttribute("success",
                            "Discount code applied successfully: " + discount.toString() + " VND");
                } else {
                    redirectAttributes.addFlashAttribute("error", vres.message);
                    return "redirect:/booking/" + roomId;
                }
            }

            redirectAttributes.addFlashAttribute("success", "Booking successful!");
            return "redirect:/my-bookings";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/booking/" + roomId;
        }
    }

    @GetMapping("/my-bookings")
    public String myBookings(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<Booking> bookings = bookingService.getUserBookings(user);
        model.addAttribute("bookings", bookings);
        return "my-bookings";
    }

    @PostMapping("/booking/{id}/cancel")
    public String cancelBooking(@PathVariable Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            bookingService.cancelBooking(id, user);
            redirectAttributes.addFlashAttribute("success", "Booking cancelled successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my-bookings";
    }

}
