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
    private BookingService bookingService;

    @Autowired
    private RoomService roomService;

    /**
     * Show review form for a booking
     */
    @GetMapping("/booking/{bookingId}/review")
    public String showReviewForm(@PathVariable Integer bookingId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Booking booking = bookingService.getBookingById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

            // Check ownership
            if (!booking.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền đánh giá đơn này");
                return "redirect:/my-bookings";
            }

            // Check if already reviewed
            Optional<Review> existingReview = reviewService.getReviewByBooking(booking);
            if (existingReview.isPresent()) {
                model.addAttribute("review", existingReview.get());
                model.addAttribute("isEdit", true);
            } else {
                model.addAttribute("isEdit", false);
            }

            model.addAttribute("booking", booking);
            model.addAttribute("room", booking.getRoom());
            return "review-form";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/my-bookings";
        }
    }

    /**
     * Submit a new review
     */
    @PostMapping("/booking/{bookingId}/review")
    public String submitReview(@PathVariable Integer bookingId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            reviewService.createReview(user, bookingId, rating, comment);
            redirectAttributes.addFlashAttribute("success", "Cảm ơn bạn đã đánh giá!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/my-bookings";
    }

    /**
     * Update an existing review
     */
    @PostMapping("/review/{reviewId}/update")
    public String updateReview(@PathVariable Integer reviewId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            reviewService.updateReview(user, reviewId, rating, comment);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật đánh giá!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/my-reviews";
    }

    /**
     * Delete a review
     */
    @PostMapping("/review/{reviewId}/delete")
    public String deleteReview(@PathVariable Integer reviewId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            reviewService.deleteReview(user, reviewId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa đánh giá!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/my-reviews";
    }

    /**
     * View all reviews for a room
     */
    @GetMapping("/room/{roomId}/reviews")
    public String viewRoomReviews(@PathVariable Integer roomId, Model model) {
        try {
            Room room = roomService.getRoomById(roomId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

            List<Review> reviews = reviewService.getReviewsByRoomId(roomId);
            Double avgRating = reviewService.getAverageRating(room);
            long reviewCount = reviewService.getReviewCount(room);

            model.addAttribute("room", room);
            model.addAttribute("reviews", reviews);
            model.addAttribute("avgRating", avgRating);
            model.addAttribute("reviewCount", reviewCount);

            return "room-reviews";
        } catch (Exception e) {
            return "redirect:/";
        }
    }

    /**
     * View my reviews
     */
    @GetMapping("/my-reviews")
    public String myReviews(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<Review> reviews = reviewService.getReviewsByUser(user);
        model.addAttribute("reviews", reviews);

        return "my-reviews";
    }
}
