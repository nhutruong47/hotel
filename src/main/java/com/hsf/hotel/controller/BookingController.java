package com.hsf.hotel.controller;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.User;
import com.hsf.hotel.service.BookingService;
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

            bookingService.createBooking(user, room, checkIn, checkOut, guestName, guestPhone, notes);
            redirectAttributes.addFlashAttribute("success", "Đặt phòng thành công!");
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
            redirectAttributes.addFlashAttribute("success", "Đã hủy đặt phòng thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my-bookings";
    }
}
