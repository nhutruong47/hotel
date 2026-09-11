package com.hsf.hotel.room.service;

import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.room.dto.InspectionDto;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.VillaInspection;
import com.hsf.hotel.room.model.VillaInspection.InspectionStatus;
import com.hsf.hotel.room.model.VillaInspection.InspectionType;
import com.hsf.hotel.room.model.VillaInspection.SeverityLevel;
import com.hsf.hotel.room.repository.RoomRepository;
import com.hsf.hotel.room.repository.VillaInspectionRepository;
import com.hsf.hotel.user.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VillaInspectionService {

    private final VillaInspectionRepository inspectionRepository;
    private final RoomRepository roomRepository;
    private final VillaMaintenanceService maintenanceService;

    public VillaInspectionService(VillaInspectionRepository inspectionRepository,
                                  RoomRepository roomRepository,
                                  VillaMaintenanceService maintenanceService) {
        this.inspectionRepository = inspectionRepository;
        this.roomRepository = roomRepository;
        this.maintenanceService = maintenanceService;
    }

    @Transactional(readOnly = true)
    public List<InspectionDto.InspectionResponse> getAllInspections() {
        return inspectionRepository.findAllByOrderByScheduledDateDesc()
                .stream()
                .map(InspectionDto.InspectionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InspectionDto.InspectionResponse> getInspectionsForRoom(Integer roomId) {
        return inspectionRepository.findByRoomIdOrderByScheduledDateDesc(roomId)
                .stream()
                .map(InspectionDto.InspectionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<InspectionDto.InspectionResponse> getInspectionById(Integer id) {
        return inspectionRepository.findById(id).map(InspectionDto.InspectionResponse::from);
    }

    @Transactional(readOnly = true)
    public Optional<InspectionDto.InspectionResponse> getLatestInspectionForRoom(Integer roomId) {
        return inspectionRepository.findLatestByRoomId(roomId).map(InspectionDto.InspectionResponse::from);
    }

    @Transactional(readOnly = true)
    public InspectionDto.InspectionStatsResponse getStats() {
        long total = inspectionRepository.count();
        long drone = inspectionRepository.countDroneMissions();
        long passed = inspectionRepository.countByStatus(InspectionStatus.PASSED);
        long flagged = inspectionRepository.countByStatus(InspectionStatus.FLAGGED_ISSUES);
        long scheduled = inspectionRepository.countByStatus(InspectionStatus.SCHEDULED);
        long inProgress = inspectionRepository.countByStatus(InspectionStatus.IN_PROGRESS);

        return new InspectionDto.InspectionStatsResponse(total, drone, passed, flagged, scheduled, inProgress);
    }

    @Transactional
    public InspectionDto.InspectionResponse createInspection(InspectionDto.InspectionRequest req, User inspector) {
        Room room = roomRepository.findById(req.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", req.roomId()));

        VillaInspection inspection = new VillaInspection();
        inspection.setRoom(room);
        inspection.setInspector(inspector);

        if (req.inspectionType() != null && !req.inspectionType().isBlank()) {
            try {
                inspection.setInspectionType(InspectionType.valueOf(req.inspectionType()));
            } catch (IllegalArgumentException e) {
                inspection.setInspectionType(InspectionType.ROUTINE_CHECK);
            }
        }

        inspection.setDroneModel(req.droneModel());
        inspection.setFlightAltitudeMeters(req.flightAltitudeMeters());
        inspection.setFlightDurationMinutes(req.flightDurationMinutes());
        inspection.setBatteryCycles(req.batteryCycles());

        if (req.status() != null && !req.status().isBlank()) {
            try {
                inspection.setStatus(InspectionStatus.valueOf(req.status()));
            } catch (IllegalArgumentException e) {
                inspection.setStatus(InspectionStatus.SCHEDULED);
            }
        }

        if (req.severityLevel() != null && !req.severityLevel().isBlank()) {
            try {
                inspection.setSeverityLevel(SeverityLevel.valueOf(req.severityLevel()));
            } catch (IllegalArgumentException e) {
                inspection.setSeverityLevel(SeverityLevel.NORMAL);
            }
        }

        inspection.setChecklistResults(req.checklistResults());
        inspection.setMediaUrls(req.mediaUrls());
        inspection.setNotes(req.notes());
        inspection.setActionRequired(req.actionRequired());
        inspection.setScheduledDate(req.scheduledDate() != null ? req.scheduledDate() : LocalDateTime.now());

        if (inspection.getStatus() == InspectionStatus.PASSED || inspection.getStatus() == InspectionStatus.FLAGGED_ISSUES) {
            inspection.setCompletedDate(LocalDateTime.now());
        }

        VillaInspection saved = inspectionRepository.save(inspection);

        // Auto-schedule maintenance if critical issues found and requested
        if (Boolean.TRUE.equals(req.autoCreateMaintenanceIfCritical()) &&
                (saved.getSeverityLevel() == SeverityLevel.CRITICAL || saved.getSeverityLevel() == SeverityLevel.HIGH)) {
            try {
                LocalDate today = LocalDate.now();
                maintenanceService.scheduleMaintenance(
                        room.getId(),
                        today,
                        today.plusDays(3),
                        "Tự động kích hoạt do phát hiện sự cố thanh tra: " + (saved.getActionRequired() != null ? saved.getActionRequired() : "Kiểm tra khẩn cấp"),
                        inspector
                );
            } catch (Exception ignored) {
                // Log and continue if maintenance scheduling has existing conflict
            }
        }

        return InspectionDto.InspectionResponse.from(saved);
    }

    @Transactional
    public InspectionDto.InspectionResponse updateInspection(Integer id, InspectionDto.InspectionRequest req, User actor) {
        VillaInspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VillaInspection", id));

        if (req.roomId() != null && !req.roomId().equals(inspection.getRoom().getId())) {
            Room room = roomRepository.findById(req.roomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room", req.roomId()));
            inspection.setRoom(room);
        }

        if (req.inspectionType() != null && !req.inspectionType().isBlank()) {
            try {
                inspection.setInspectionType(InspectionType.valueOf(req.inspectionType()));
            } catch (IllegalArgumentException ignored) {}
        }

        if (req.droneModel() != null) inspection.setDroneModel(req.droneModel());
        if (req.flightAltitudeMeters() != null) inspection.setFlightAltitudeMeters(req.flightAltitudeMeters());
        if (req.flightDurationMinutes() != null) inspection.setFlightDurationMinutes(req.flightDurationMinutes());
        if (req.batteryCycles() != null) inspection.setBatteryCycles(req.batteryCycles());

        if (req.status() != null && !req.status().isBlank()) {
            try {
                InspectionStatus newStatus = InspectionStatus.valueOf(req.status());
                inspection.setStatus(newStatus);
                if (newStatus == InspectionStatus.PASSED || newStatus == InspectionStatus.FLAGGED_ISSUES) {
                    if (inspection.getCompletedDate() == null) {
                        inspection.setCompletedDate(LocalDateTime.now());
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        }

        if (req.severityLevel() != null && !req.severityLevel().isBlank()) {
            try {
                inspection.setSeverityLevel(SeverityLevel.valueOf(req.severityLevel()));
            } catch (IllegalArgumentException ignored) {}
        }

        if (req.checklistResults() != null) inspection.setChecklistResults(req.checklistResults());
        if (req.mediaUrls() != null) inspection.setMediaUrls(req.mediaUrls());
        if (req.notes() != null) inspection.setNotes(req.notes());
        if (req.actionRequired() != null) inspection.setActionRequired(req.actionRequired());
        if (req.scheduledDate() != null) inspection.setScheduledDate(req.scheduledDate());

        VillaInspection saved = inspectionRepository.save(inspection);
        return InspectionDto.InspectionResponse.from(saved);
    }

    @Transactional
    public void deleteInspection(Integer id) {
        if (!inspectionRepository.existsById(id)) {
            throw new ResourceNotFoundException("VillaInspection", id);
        }
        inspectionRepository.deleteById(id);
    }
}
