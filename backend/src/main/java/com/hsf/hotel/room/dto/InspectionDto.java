package com.hsf.hotel.room.dto;

import com.hsf.hotel.room.model.VillaInspection;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class InspectionDto {

    public record InspectionResponse(
            Integer id,
            Integer roomId,
            String roomNumber,
            String roomTypeName,
            Integer inspectorId,
            String inspectorName,
            String inspectionType,
            String droneModel,
            Double flightAltitudeMeters,
            Integer flightDurationMinutes,
            Integer batteryCycles,
            String status,
            String severityLevel,
            String checklistResults,
            String mediaUrls,
            String notes,
            String actionRequired,
            LocalDateTime scheduledDate,
            LocalDateTime completedDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static InspectionResponse from(VillaInspection i) {
            if (i == null) return null;
            return new InspectionResponse(
                    i.getId(),
                    i.getRoom() != null ? i.getRoom().getId() : null,
                    i.getRoom() != null ? i.getRoom().getRoomNumber() : null,
                    i.getRoom() != null && i.getRoom().getRoomType() != null ? i.getRoom().getRoomType().getName() : null,
                    i.getInspector() != null ? i.getInspector().getId() : null,
                    i.getInspector() != null ? (i.getInspector().getFullName() != null ? i.getInspector().getFullName() : i.getInspector().getUsername()) : null,
                    i.getInspectionType() != null ? i.getInspectionType().name() : null,
                    i.getDroneModel(),
                    i.getFlightAltitudeMeters(),
                    i.getFlightDurationMinutes(),
                    i.getBatteryCycles(),
                    i.getStatus() != null ? i.getStatus().name() : null,
                    i.getSeverityLevel() != null ? i.getSeverityLevel().name() : null,
                    i.getChecklistResults(),
                    i.getMediaUrls(),
                    i.getNotes(),
                    i.getActionRequired(),
                    i.getScheduledDate(),
                    i.getCompletedDate(),
                    i.getCreatedAt(),
                    i.getUpdatedAt()
            );
        }
    }

    public record InspectionRequest(
            @NotNull(message = "Room ID is required")
            Integer roomId,
            String inspectionType,
            String droneModel,
            Double flightAltitudeMeters,
            Integer flightDurationMinutes,
            Integer batteryCycles,
            String status,
            String severityLevel,
            String checklistResults,
            String mediaUrls,
            String notes,
            String actionRequired,
            LocalDateTime scheduledDate,
            Boolean autoCreateMaintenanceIfCritical
    ) {}

    public record InspectionStatsResponse(
            long totalInspections,
            long droneMissions,
            long passedInspections,
            long flaggedIssues,
            long scheduledCount,
            long inProgressCount
    ) {}
}
