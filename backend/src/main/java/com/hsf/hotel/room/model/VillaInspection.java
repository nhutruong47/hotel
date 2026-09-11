package com.hsf.hotel.room.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hsf.hotel.user.model.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "villa_inspections", indexes = {
    @Index(name = "idx_inspections_room", columnList = "room_id"),
    @Index(name = "idx_inspections_status", columnList = "status"),
    @Index(name = "idx_inspections_type", columnList = "inspection_type"),
    @Index(name = "idx_inspections_scheduled_date", columnList = "scheduled_date")
})
public class VillaInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "bookings", "amenities", "galleryImages"})
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "verificationToken", "resetToken"})
    private User inspector;

    @Enumerated(EnumType.STRING)
    @Column(name = "inspection_type", length = 50, nullable = false)
    private InspectionType inspectionType = InspectionType.ROUTINE_CHECK;

    @Column(name = "drone_model", length = 100)
    private String droneModel;

    @Column(name = "flight_altitude_meters")
    private Double flightAltitudeMeters;

    @Column(name = "flight_duration_minutes")
    private Integer flightDurationMinutes;

    @Column(name = "battery_cycles")
    private Integer batteryCycles;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private InspectionStatus status = InspectionStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity_level", length = 20, nullable = false)
    private SeverityLevel severityLevel = SeverityLevel.NORMAL;

    @Column(name = "checklist_results", columnDefinition = "TEXT")
    private String checklistResults;

    @Column(name = "media_urls", columnDefinition = "TEXT")
    private String mediaUrls;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "action_required", length = 500)
    private String actionRequired;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate = LocalDateTime.now();

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum InspectionType {
        ROUTINE_CHECK,
        PRE_CHECKIN,
        POST_CHECKOUT,
        DRONE_ROOF_SURVEY,
        DRONE_FACADE_SURVEY,
        THERMAL_FACADE,
        POOL_FACILITY,
        MAINTENANCE_AUDIT
    }

    public enum InspectionStatus {
        SCHEDULED,
        IN_PROGRESS,
        PASSED,
        FLAGGED_ISSUES,
        REPAIRED,
        CANCELLED
    }

    public enum SeverityLevel {
        NORMAL,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public VillaInspection() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (scheduledDate == null) scheduledDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public User getInspector() { return inspector; }
    public void setInspector(User inspector) { this.inspector = inspector; }
    public InspectionType getInspectionType() { return inspectionType; }
    public void setInspectionType(InspectionType inspectionType) { this.inspectionType = inspectionType; }
    public String getDroneModel() { return droneModel; }
    public void setDroneModel(String droneModel) { this.droneModel = droneModel; }
    public Double getFlightAltitudeMeters() { return flightAltitudeMeters; }
    public void setFlightAltitudeMeters(Double flightAltitudeMeters) { this.flightAltitudeMeters = flightAltitudeMeters; }
    public Integer getFlightDurationMinutes() { return flightDurationMinutes; }
    public void setFlightDurationMinutes(Integer flightDurationMinutes) { this.flightDurationMinutes = flightDurationMinutes; }
    public Integer getBatteryCycles() { return batteryCycles; }
    public void setBatteryCycles(Integer batteryCycles) { this.batteryCycles = batteryCycles; }
    public InspectionStatus getStatus() { return status; }
    public void setStatus(InspectionStatus status) { this.status = status; }
    public SeverityLevel getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(SeverityLevel severityLevel) { this.severityLevel = severityLevel; }
    public String getChecklistResults() { return checklistResults; }
    public void setChecklistResults(String checklistResults) { this.checklistResults = checklistResults; }
    public String getMediaUrls() { return mediaUrls; }
    public void setMediaUrls(String mediaUrls) { this.mediaUrls = mediaUrls; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getActionRequired() { return actionRequired; }
    public void setActionRequired(String actionRequired) { this.actionRequired = actionRequired; }
    public LocalDateTime getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDateTime scheduledDate) { this.scheduledDate = scheduledDate; }
    public LocalDateTime getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDateTime completedDate) { this.completedDate = completedDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
