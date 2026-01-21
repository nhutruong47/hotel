package com.hsf.hotel.controller;

import java.util.HashMap;
import java.util.Map;
import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.RoomType;
import com.hsf.hotel.model.User;
import com.hsf.hotel.service.RoomService;
import com.hsf.hotel.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<Room> rooms = roomService.getAvailableRooms();
        
        // Create maps to hold rating information
        Map<Integer, Double> roomRatings = new HashMap<>();
        Map<Integer, Long> roomReviewCounts = new HashMap<>();
        
        // Populate rating information for each room
        for (Room room : rooms) {
            roomRatings.put(room.getId(), reviewService.getAverageRatingForRoom(room));
            roomReviewCounts.put(room.getId(), reviewService.countReviewsForRoom(room));
        }
        
        model.addAttribute("rooms", rooms);
        model.addAttribute("roomRatings", roomRatings);
        model.addAttribute("roomReviewCounts", roomReviewCounts);
        model.addAttribute("roomTypes", RoomType.values());
        return "index";
    }

    @GetMapping("/rooms")
    public String listRooms(@RequestParam(required = false) RoomType type,
            HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<Room> rooms;
        if (type != null) {
            rooms = roomService.getRoomsByType(type);
        } else {
            rooms = roomService.getAvailableRooms();
        }

        model.addAttribute("rooms", rooms);
        model.addAttribute("roomTypes", RoomType.values());
        model.addAttribute("selectedType", type);
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
                    return "room-detail";
                })
                .orElse("redirect:/");
    }
}
