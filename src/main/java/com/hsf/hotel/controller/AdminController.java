package com.hsf.hotel.controller;

import com.hsf.hotel.model.*;
import com.hsf.hotel.repository.UserRepository;
import com.hsf.hotel.service.BookingService;
import com.hsf.hotel.service.RoomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserRepository userRepository;

    // Check if user is admin
    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user != null && "ADMIN".equals(user.getRole());
    }

    // Dashboard
    @GetMapping
    public String dashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        // Statistics
        List<Room> allRooms = roomService.getAllRooms();
        List<Room> availableRooms = roomService.getAvailableRooms();
        List<Booking> allBookings = bookingService.getAllBookings();
        List<Booking> todayBookings = bookingService.getTodayBookings();
        BigDecimal monthlyRevenue = bookingService.getMonthlyRevenue();
        long pendingCount = bookingService.getPendingBookingsCount();

        model.addAttribute("totalRooms", allRooms.size());
        model.addAttribute("availableRooms", availableRooms.size());
        model.addAttribute("totalBookings", allBookings.size());
        model.addAttribute("todayBookings", todayBookings.size());
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("recentBookings", allBookings.stream().limit(5).toList());

        // Reports
        model.addAttribute("mostBookedRooms", bookingService.getMostBookedRooms());
        model.addAttribute("mostRatingRooms", bookingService.getMostRatingRooms());
        return "admin-dashboard";
    }

    // Booking Management
    @GetMapping("/bookings")
    public String bookings(HttpSession session, Model model,
            @RequestParam(required = false) String status) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        List<Booking> bookings;
        if (status != null && !status.isEmpty()) {
            bookings = bookingService.getBookingsByStatus(BookingStatus.valueOf(status));
        } else {
            bookings = bookingService.getAllBookings();
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("selectedStatus", status);

        return "admin-bookings";
    }

    @PostMapping("/booking/{id}/status")
    public String updateBookingStatus(@PathVariable Integer id,
            @RequestParam String status,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            bookingService.updateBookingStatus(id, BookingStatus.valueOf(status));
            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/bookings";
    }

    /**
     * WORKFLOW: Admin duyệt booking
     */
    @PostMapping("/booking/{id}/approve")
    public String approveBooking(@PathVariable Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            User admin = (User) session.getAttribute("user");
            bookingService.approveBooking(id, admin);
            redirectAttributes.addFlashAttribute("success", "Đã duyệt đơn đặt phòng #" + id);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/bookings";
    }

    /**
     * WORKFLOW: Admin từ chối booking
     */
    @PostMapping("/booking/{id}/reject")
    public String rejectBooking(@PathVariable Integer id,
            @RequestParam(required = false) String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            User admin = (User) session.getAttribute("user");
            bookingService.rejectBooking(id, admin, reason);
            redirectAttributes.addFlashAttribute("success", "Đã từ chối đơn đặt phòng #" + id);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/bookings";
    }

    /**
     * WORKFLOW: Xác nhận thanh toán
     */
    @PostMapping("/booking/{id}/confirm-payment")
    public String confirmPayment(@PathVariable Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            bookingService.confirmPayment(id);
            redirectAttributes.addFlashAttribute("success", "Đã xác nhận thanh toán cho đơn #" + id);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/bookings";
    }

    // Room Management
    @GetMapping("/rooms")
    public String rooms(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("roomTypes", RoomType.values());

        return "admin-rooms";
    }

    @PostMapping("/room")
    public String saveRoom(@RequestParam(required = false) Integer id,
            @RequestParam String roomNumber,
            @RequestParam String roomType,
            @RequestParam BigDecimal pricePerNight,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false, defaultValue = "true") Boolean isAvailable,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            Room room;
            if (id != null) {
                room = roomService.getRoomById(id)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));
            } else {
                room = new Room();
            }

            room.setRoomNumber(roomNumber);
            room.setRoomType(RoomType.valueOf(roomType));
            room.setPricePerNight(pricePerNight);
            room.setDescription(description);
            room.setImageUrl(imageUrl);
            room.setIsAvailable(isAvailable);

            roomService.saveRoom(room);
            redirectAttributes.addFlashAttribute("success",
                    id != null ? "Cập nhật phòng thành công!" : "Thêm phòng mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/rooms";
    }

    @PostMapping("/room/{id}/delete")
    public String deleteRoom(@PathVariable Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            roomService.deleteRoom(id);
            redirectAttributes.addFlashAttribute("success", "Xóa phòng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/rooms";
    }

    // User Management
    @GetMapping("/users")
    public String users(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("users", userRepository.findAll());

        return "admin-users";
    }

    @PostMapping("/user/{id}/role")
    public String updateUserRole(@PathVariable Integer id,
            @RequestParam String role,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
            user.setRole(role);
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("success", "Cập nhật quyền thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/user/{id}/delete")
    public String deleteUser(@PathVariable Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            User currentUser = (User) session.getAttribute("user");
            if (currentUser.getId().equals(id)) {
                throw new RuntimeException("Không thể xóa chính mình!");
            }
            userRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Xóa user thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @GetMapping("/reports")
    public String reports(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("mostBookedRooms", bookingService.getMostBookedRooms());
        model.addAttribute("topRatedRooms", bookingService.getMostRatingRooms());

        return "admin-mostbookingandrating";
    }
}
