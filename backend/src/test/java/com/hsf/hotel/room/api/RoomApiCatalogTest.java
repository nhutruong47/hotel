package com.hsf.hotel.room.api;

import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.review.service.ReviewService;
import com.hsf.hotel.room.dto.RoomCatalogResponse;
import com.hsf.hotel.room.dto.RoomDTO;
import com.hsf.hotel.room.dto.RoomMapper;
import com.hsf.hotel.room.service.RoomQueryService;
import com.hsf.hotel.room.service.RoomService;
import com.hsf.hotel.room.service.RoomTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomApiCatalogTest {

    @Mock private RoomService roomService;
    @Mock private RoomQueryService roomQueryService;
    @Mock private RoomTypeService roomTypeService;
    @Mock private ReviewService reviewService;
    @Mock private BookingRepository bookingRepository;
    @Mock private RoomMapper roomMapper;

    private RoomApi roomApi;

    @BeforeEach
    void setUp() {
        roomApi = new RoomApi(roomService, roomQueryService, roomTypeService,
                reviewService, bookingRepository, roomMapper);
    }

    @Test
    void catalogueUsesBoundedPaginationAndAllowlistedSort() {
        RoomDTO room = new RoomDTO();
        room.setId(9);
        when(roomQueryService.search(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                anyList(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(room), PageRequest.of(1, 2), 5));
        when(roomTypeService.getAllRoomTypes()).thenReturn(List.of());
        when(roomMapper.roomTypesToRoomTypeDTOs(List.of())).thenReturn(List.of());

        ResponseEntity<ApiResponse<RoomCatalogResponse>> response = roomApi.getRooms(
                null, null, null, null, null, null, null, List.of(" Pool ", "Pool"),
                null, 1, 2, "price-desc");

        RoomCatalogResponse data = response.getBody().getData();
        assertEquals(List.of(room), data.rooms());
        assertEquals(5, data.totalItems());
        assertEquals(3, data.totalPages());
        assertEquals("price-desc", data.sort());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(roomQueryService).search(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(List.of("Pool")), isNull(), pageable.capture());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(2, pageable.getValue().getPageSize());
        assertEquals("DESC", pageable.getValue().getSort().getOrderFor("pricePerNight").getDirection().name());
    }

    @Test
    void catalogueRejectsUnboundedOrInvalidSearches() {
        assertThrows(IllegalArgumentException.class, () -> roomApi.getRooms(
                LocalDate.now(), null, null, null, null, null, null, null,
                null, 0, 24, "recommended"));
        assertThrows(IllegalArgumentException.class, () -> roomApi.getRooms(
                null, null, BigDecimal.TEN, BigDecimal.ONE, null, null, null, null,
                null, 0, 24, "recommended"));
        assertThrows(IllegalArgumentException.class, () -> roomApi.getRooms(
                null, null, null, null, null, null, null, null,
                null, 0, 101, "recommended"));
        assertThrows(IllegalArgumentException.class, () -> roomApi.getRooms(
                null, null, null, null, null, null, null, null,
                null, 0, 24, "drop-table"));
    }
}
