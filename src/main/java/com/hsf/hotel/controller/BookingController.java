package com.hsf.hotel.controller;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.Review;
import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.User;
import com.hsf.hotel.service.BookingService;
import com.hsf.hotel.service.ReviewService;
import com.hsf.hotel.service.RoomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.util.List;

@Controller
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ReviewService reviewService;

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

    @PostMapping("/booking")
    public String createBooking(@RequestParam Integer roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam String guestName,
            @RequestParam(required = false) String guestPhone,
            @RequestParam(required = false) String notes,
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
        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            redirectAttributes.addFlashAttribute("error", "Ngày check-out phải sau ngày check-in");
            return "redirect:/booking/" + roomId;
        }

        try {
            Room room = roomService.getRoomById(roomId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

            Booking booking = bookingService.createBooking(user, room, checkIn, checkOut, guestName, guestPhone, notes);
            redirectAttributes.addFlashAttribute("success", "Đặt phòng thành công! Vui lòng thanh toán để hoàn tất.");
            return "redirect:/booking/" + booking.getId() + "/payment";
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
            redirectAttributes.addFlashAttribute("success", "Đã hủy đặt phòng thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my-bookings";
    }

    /**
     * WORKFLOW: User thanh toán booking
     * Giả lập thanh toán - trong thực tế sẽ tích hợp VNPay/Momo
     */
    @GetMapping("/booking/{id}/payment")
    public String paymentPage(@PathVariable Integer id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Booking booking = bookingService.getBookingById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

            // Check ownership
            if (!booking.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thanh toán đơn này");
                return "redirect:/my-bookings";
            }

            // Check status
            if (!booking.getStatus().name().equals("AWAITING_PAYMENT")) {
                redirectAttributes.addFlashAttribute("error", "Đơn này không ở trạng thái chờ thanh toán");
                return "redirect:/my-bookings";
            }

            model.addAttribute("booking", booking);
            return "payment";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/my-bookings";
        }
    }

    /**
     * WORKFLOW: Xác nhận thanh toán (giả lập)
     */
    @PostMapping("/booking/{id}/pay")
    public String processPayment(@PathVariable Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Booking booking = bookingService.getBookingById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

            // Check ownership
            if (!booking.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Bạn không có quyền thanh toán đơn này");
            }

            // Process payment (simulated)
            bookingService.confirmPayment(id);
            redirectAttributes.addFlashAttribute("success", "Thanh toán thành công! Đặt phòng đã được xác nhận.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my-bookings";
    }
}
