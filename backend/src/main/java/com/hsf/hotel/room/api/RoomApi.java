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
import com.hsf.hotel.room.dto.RoomCatalogResponse;
import com.hsf.hotel.room.dto.RoomTypeDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    public ResponseEntity<ApiResponse<RoomCatalogResponse>> getRooms(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer roomTypeId,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(required = false) List<String> amenities,
            @RequestParam(required = false) String promotion,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(defaultValue = "recommended") String sort) {

        validateSearch(checkIn, checkOut, minPrice, maxPrice, capacity, bedrooms, page, size);
        String canonicalSort = canonicalSort(sort);
        List<String> normalizedAmenities = amenities == null ? List.of() : amenities.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        PageRequest pageable = PageRequest.of(page, size, resolveSort(canonicalSort));
        Page<RoomDTO> roomDTOs = roomQueryService.search(
                checkIn, checkOut, minPrice, maxPrice, roomTypeId,
                capacity, bedrooms, normalizedAmenities, promotion, pageable);
        List<RoomTypeDTO> roomTypeDTOs = roomMapper.roomTypesToRoomTypeDTOs(roomTypeService.getAllRoomTypes());
        return ResponseEntity.ok(ApiResponse.ok(RoomCatalogResponse.from(roomDTOs, roomTypeDTOs, canonicalSort)));
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
        detail.put("reviews", reviewService.getPublicReviewsByRoom(room));
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

    private static void validateSearch(
            LocalDate checkIn,
            LocalDate checkOut,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer capacity,
            Integer bedrooms,
            int page,
            int size
    ) {
        if ((checkIn == null) != (checkOut == null)) {
            throw new IllegalArgumentException("checkIn and checkOut must be provided together");
        }
        if (checkIn != null && !checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
        if (minPrice != null && minPrice.signum() < 0
                || maxPrice != null && maxPrice.signum() < 0
                || minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Invalid price range");
        }
        if (capacity != null && capacity < 1 || bedrooms != null && bedrooms < 1) {
            throw new IllegalArgumentException("Capacity and bedrooms must be positive");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        }
    }

    private static String canonicalSort(String sort) {
        String value = sort == null ? "recommended" : sort.trim().toLowerCase();
        return switch (value) {
            case "recommended", "price-asc", "price-desc", "capacity-desc", "rating-desc" -> value;
            default -> throw new IllegalArgumentException("Unsupported room sort");
        };
    }

    private static Sort resolveSort(String sort) {
        return switch (sort) {
            case "price-asc" -> Sort.by(Sort.Order.asc("pricePerNight"), Sort.Order.asc("id"));
            case "price-desc" -> Sort.by(Sort.Order.desc("pricePerNight"), Sort.Order.asc("id"));
            case "capacity-desc" -> Sort.by(Sort.Order.desc("capacity"), Sort.Order.asc("id"));
            case "rating-desc", "recommended" -> Sort.by(
                    Sort.Order.desc("avgRating").nullsLast(),
                    Sort.Order.desc("reviewCount"),
                    Sort.Order.asc("id"));
            default -> throw new IllegalArgumentException("Unsupported room sort");
        };
    }
}
