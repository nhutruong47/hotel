package com.hsf.hotel.room.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.review.service.ReviewService;
import com.hsf.hotel.room.service.RoomService;
import com.hsf.hotel.room.service.RoomQueryService;
import com.hsf.hotel.room.service.RoomTypeService;
import com.hsf.hotel.room.dto.RoomMapper;
import com.hsf.hotel.room.dto.RoomDTO;
import com.hsf.hotel.room.dto.RoomTypeDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@com.hsf.hotel.config.ApiController
@RequestMapping(com.hsf.hotel.config.ApiPaths.V1 + "/rooms")
public class RoomApi {

    private final RoomService roomService;
    private final RoomQueryService roomQueryService;
    private final RoomTypeService roomTypeService;
    private final ReviewService reviewService;
    private final BookingRepository bookingRepository;
    private final RoomMapper roomMapper;

    public RoomApi(RoomService roomService,
                   RoomQueryService roomQueryService,
                   RoomTypeService roomTypeService,
                   ReviewService reviewService,
                   BookingRepository bookingRepository,
                   RoomMapper roomMapper) {
        this.roomService = roomService;
        this.roomQueryService = roomQueryService;
        this.roomTypeService = roomTypeService;
        this.reviewService = reviewService;
        this.bookingRepository = bookingRepository;
        this.roomMapper = roomMapper;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getRooms(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer roomTypeId,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(required = false) List<String> amenities,
            @RequestParam(required = false) String promotion) {

        boolean unfiltered = checkIn == null && checkOut == null
                && minPrice == null && maxPrice == null && roomTypeId == null
                && capacity == null && bedrooms == null && (amenities == null || amenities.isEmpty())
                && (promotion == null || promotion.isEmpty());
                
        List<RoomDTO> roomDTOs = unfiltered
                ? roomQueryService.available()
                : roomQueryService.search(checkIn, checkOut, minPrice, maxPrice, roomTypeId,
                        capacity, bedrooms, amenities, promotion);
        List<RoomTypeDTO> roomTypeDTOs = roomMapper.roomTypesToRoomTypeDTOs(roomTypeService.getAllRoomTypes());

        Map<String, Object> data = new HashMap<>();
        data.put("rooms", roomDTOs);
        data.put("roomTypes", roomTypeDTOs);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getRoomDetail(@PathVariable Integer id) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id));
        
        RoomDTO roomDTO = roomQueryService.byId(id);
        List<RoomTypeDTO> roomTypeDTOs = roomMapper.roomTypesToRoomTypeDTOs(roomTypeService.getAllRoomTypes());

        Map<String, Object> detail = new HashMap<>();
        detail.put("room", roomDTO);
        detail.put("avgRating", reviewService.getAverageRating(room));
        detail.put("reviewCount", reviewService.getReviewCount(room));
        detail.put("reviews", reviewService.getReviewsByRoom(room));
        detail.put("roomTypes", roomTypeDTOs);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    @GetMapping("/{roomId}/booked-dates")
    public ResponseEntity<ApiResponse<?>> getBookedDates(@PathVariable Integer roomId) {
        List<Booking> active = bookingRepository.findActiveBookingsByRoomId(roomId);
        List<Map<String, String>> ranges = active.stream().map(b -> {
            Map<String, String> r = new HashMap<>();
            r.put("checkIn", b.getCheckInDate().toString());
            r.put("checkOut", b.getCheckOutDate().toString());
            r.put("status", b.getStatus().name());
            return r;
        }).toList();
        return ResponseEntity.ok(ApiResponse.ok(ranges));
    }
}
