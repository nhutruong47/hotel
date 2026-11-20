package com.hsf.hotel.room.dto;

import com.hsf.hotel.room.model.VillaMaintenance;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** API view of a villa maintenance window (P0-04). Shape matches the admin SPA's VillaMaintenance type. */
public record MaintenanceResponse(
        Integer id,
        RoomRef room,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        String status,
        UserRef createdBy,
        LocalDateTime createdAt
) {
    public record RoomRef(Integer id, String roomNumber) {}

    public record UserRef(Integer id, String username, String fullName) {}

    public static MaintenanceResponse from(VillaMaintenance m) {
        if (m == null) return null;
        return new MaintenanceResponse(
                m.getId(),
                m.getRoom() == null ? null : new RoomRef(m.getRoom().getId(), m.getRoom().getRoomNumber()),
                m.getStartDate(),
                m.getEndDate(),
                m.getReason(),
                m.getStatus() != null ? m.getStatus().name() : null,
                m.getCreatedBy() == null ? null : new UserRef(m.getCreatedBy().getId(),
                        m.getCreatedBy().getUsername(), m.getCreatedBy().getFullName()),
                m.getCreatedAt()
        );
    }
}
