package com.hsf.hotel.controller;

import com.hsf.hotel.model.Review;
import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.User;
import com.hsf.hotel.service.RoomTypeService;
import com.hsf.hotel.service.ReviewService;
import com.hsf.hotel.service.RoomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;

@Controller
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<Room> rooms = roomService.getAvailableRooms();
        model.addAttribute("rooms", rooms);
        model.addAttribute("roomTypes", roomTypeService.getAllRoomTypes());
        return "index";
    }

    @GetMapping("/rooms")
    public String listRooms(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer roomTypeId,
            HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // If no search params are provided, just show all available rooms
        List<Room> rooms;
        if (checkIn == null && checkOut == null && minPrice == null && maxPrice == null && roomTypeId == null) {
            rooms = roomService.getAvailableRooms();
        } else {
            rooms = roomService.searchRooms(checkIn, checkOut, minPrice, maxPrice, roomTypeId);
        }

        model.addAttribute("rooms", rooms);
        model.addAttribute("roomTypes", roomTypeService.getAllRoomTypes());

        // Pass search filters back to the view
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("selectedTypeId", roomTypeId);

        return "index";
    }

    @GetMapping("/rooms/{id}")
    public String roomDetail(@PathVariable Integer id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        return roomService.getRoomById(id)
                .map(room -> {
                    model.addAttribute("room", room);

                    // Review data
                    Double avgRating = reviewService.getAverageRating(room);
                    long reviewCount = reviewService.getReviewCount(room);
                    List<Review> reviews = reviewService.getReviewsByRoom(room);
                    model.addAttribute("avgRating", avgRating);
                    model.addAttribute("reviewCount", reviewCount);
                    model.addAttribute("reviews", reviews);

                    return "room-detail";
                })
                .orElse("redirect:/");
    }
}
