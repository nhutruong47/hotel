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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private BookingService bookingService;

    @GetMapping("/rooms/{roomId}/reviews")
    public String roomReviews(@PathVariable Integer roomId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Optional<Room> roomOpt = roomService.getRoomById(roomId);
        if (roomOpt.isEmpty()) {
            return "redirect:/";
        }

        Room room = roomOpt.get();
        List<Review> reviews = reviewService.getPublicReviewsByRoom(room);
        Double avgRating = reviewService.getAverageRatingForRoom(room);
        Long reviewCount = reviewService.countPublicReviewsForRoom(room);

        model.addAttribute("room", room);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("user", user);

        return "room-reviews";
    }

    @GetMapping("/review/new")
    public String createReviewForm(@RequestParam(required = false) Integer roomId,
                                  @RequestParam(required = false) Integer bookingId,
                                  HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<Booking> eligibleBookings = reviewService.getEligibleBookingsForReview(user);
        
        // If specific booking is provided, check if it's eligible
        Booking selectedBooking = null;
        Room selectedRoom = null;
        
        if (bookingId != null) {
            Optional<Booking> bookingOpt = bookingService.getBookingById(bookingId);
            if (bookingOpt.isPresent() && reviewService.isBookingEligibleForReview(bookingOpt.get())) {
                selectedBooking = bookingOpt.get();
                selectedRoom = selectedBooking.getRoom();
            }
        } else if (roomId != null) {
            Optional<Room> roomOpt = roomService.getRoomById(roomId);
            if (roomOpt.isPresent()) {
                selectedRoom = roomOpt.get();
                // Find eligible booking for this room
                selectedBooking = eligibleBookings.stream()
                        .filter(b -> b.getRoom().getId().equals(roomId))
                        .findFirst()
                        .orElse(null);
            }
        }

        model.addAttribute("eligibleBookings", eligibleBookings);
        model.addAttribute("selectedBooking", selectedBooking);
        model.addAttribute("selectedRoom", selectedRoom);
        model.addAttribute("user", user);

        return "review-form";
    }

    @PostMapping("/review/create")
    public String createReview(@RequestParam Integer bookingId,
                              @RequestParam Integer rating,
                              @RequestParam String comment,
                              @RequestParam(defaultValue = "false") boolean anonymous,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Optional<Booking> bookingOpt = bookingService.getBookingById(bookingId);
            if (bookingOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin đặt phòng");
                return "redirect:/review/new";
            }

            Booking booking = bookingOpt.get();
            
            // Check if user can review this booking
            if (!reviewService.isBookingEligibleForReview(booking)) {
                redirectAttributes.addFlashAttribute("error", "Bạn chỉ có thể đánh giá các đặt phòng đã hoàn thành");
                return "redirect:/my-bookings";
            }

            if (reviewService.hasUserReviewedBooking(user, booking)) {
                redirectAttributes.addFlashAttribute("error", "Bạn đã đánh giá đặt phòng này rồi");
                return "redirect:/my-bookings";
            }

            Review review = reviewService.createReview(
                    user, booking.getRoom(), booking, rating, comment, anonymous);

            redirectAttributes.addFlashAttribute("success", "Đánh giá của bạn đã được gửi thành công!");
            return "redirect:/rooms/" + booking.getRoom().getId() + "/reviews";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/review/new?bookingId=" + bookingId;
        }
    }

    @GetMapping("/review/{id}/edit")
    public String editReviewForm(@PathVariable Integer id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Optional<Review> reviewOpt = reviewService.getReviewById(id);
        if (reviewOpt.isEmpty()) {
            return "redirect:/my-bookings";
        }

        Review review = reviewOpt.get();
        
        // Check if user owns this review
        if (!review.getUser().getId().equals(user.getId())) {
            return "redirect:/my-bookings";
        }

        model.addAttribute("review", review);
        model.addAttribute("user", user);

        return "review-form";
    }

    @PostMapping("/review/{id}/update")
    public String updateReview(@PathVariable Integer id,
                              @RequestParam Integer rating,
                              @RequestParam String comment,
                              @RequestParam(defaultValue = "false") boolean anonymous,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Review review = reviewService.updateReview(id, user, rating, comment, anonymous);
            redirectAttributes.addFlashAttribute("success", "Đánh giá đã được cập nhật thành công!");
            return "redirect:/my-reviews";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/review/" + id + "/edit";
        }
    }

    @PostMapping("/review/{id}/delete")
    public String deleteReview(@PathVariable Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Optional<Review> reviewOpt = reviewService.getReviewById(id);
            if (reviewOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy đánh giá");
                return "redirect:/my-bookings";
            }

            Review review = reviewOpt.get();
            Integer roomId = review.getRoom().getId();
            
            reviewService.deleteReview(id, user);
            redirectAttributes.addFlashAttribute("success", "Đánh giá đã được xóa thành công!");
            return "redirect:/my-reviews";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/my-bookings";
        }
    }

    @GetMapping("/my-reviews")
    public String myReviews(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<Review> reviews = reviewService.getReviewsByUser(user);
        model.addAttribute("reviews", reviews);
        model.addAttribute("user", user);

        return "my-reviews";
    }
}