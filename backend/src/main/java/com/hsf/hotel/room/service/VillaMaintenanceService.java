package com.hsf.hotel.room.service;

import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.VillaInspection;
import com.hsf.hotel.room.model.VillaMaintenance;
import com.hsf.hotel.room.model.VillaMaintenance.MaintenanceStatus;
import com.hsf.hotel.room.repository.RoomRepository;
import com.hsf.hotel.room.repository.VillaInspectionRepository;
import com.hsf.hotel.room.repository.VillaMaintenanceRepository;
import com.hsf.hotel.user.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VillaMaintenanceService {

    private final VillaMaintenanceRepository maintenanceRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final VillaInspectionRepository inspectionRepository;

    public VillaMaintenanceService(VillaMaintenanceRepository maintenanceRepository,
                                   RoomRepository roomRepository,
                                   BookingRepository bookingRepository,
                                   VillaInspectionRepository inspectionRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.inspectionRepository = inspectionRepository;
    }

    @Transactional(readOnly = true)
    public List<VillaMaintenance> getAllMaintenances() {
        return maintenanceRepository.findAllByOrderByStartDateDesc();
    }

    @Transactional(readOnly = true)
    public List<VillaMaintenance> getMaintenancesForRoom(Integer roomId) {
        return maintenanceRepository.findByRoomIdOrderByStartDateDesc(roomId);
    }

    @Transactional(readOnly = true)
    public boolean isRoomInMaintenance(Room room, LocalDate checkIn, LocalDate checkOut) {
        return !maintenanceRepository.findConflictingMaintenances(room, checkIn, checkOut).isEmpty();
    }

    @Transactional
    public VillaMaintenance scheduleMaintenance(Integer roomId, LocalDate startDate, LocalDate endDate,
                                                String reason, User adminUser) {
        if (startDate == null || endDate == null) {
            throw new BusinessRuleException("INVALID_DATES", "Ngày bắt đầu và ngày kết thúc bảo trì không được để trống");
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException("INVALID_DATE_RANGE", "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu bảo trì");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

        // Check if there are active bookings overlapping with this maintenance schedule
        List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(room, startDate, endDate);
        if (!conflictingBookings.isEmpty()) {
            throw new BusinessRuleException("MAINTENANCE_BOOKING_CONFLICT",
                    "Không thể lên lịch bảo trì: Đã có " + conflictingBookings.size() +
                    " đơn đặt phòng của khách trong khoảng thời gian này!");
        }

        // Check if there is already a scheduled maintenance in this period
        List<VillaMaintenance> conflicts = maintenanceRepository.findConflictingMaintenances(room, startDate, endDate);
        if (!conflicts.isEmpty()) {
            throw new BusinessRuleException("MAINTENANCE_OVERLAP",
                    "Đã có lịch bảo trì khác được thiết lập trong khoảng thời gian này");
        }

        VillaMaintenance maintenance = new VillaMaintenance(room, startDate, endDate, reason, adminUser);
        return maintenanceRepository.save(maintenance);
    }

    @Transactional
    public VillaMaintenance updateStatus(Integer maintenanceId, MaintenanceStatus status) {
        VillaMaintenance m = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("VillaMaintenance", maintenanceId));
        MaintenanceStatus previousStatus = m.getStatus();
        m.setStatus(status);
        VillaMaintenance saved = maintenanceRepository.save(m);

        // Auto-schedule quality re-inspection when maintenance is marked COMPLETED
        if (status == MaintenanceStatus.COMPLETED && previousStatus != MaintenanceStatus.COMPLETED) {
            VillaInspection reinspection = new VillaInspection();
            reinspection.setRoom(m.getRoom());
            reinspection.setInspectionType(VillaInspection.InspectionType.ROUTINE_CHECK);
            reinspection.setStatus(VillaInspection.InspectionStatus.SCHEDULED);
            reinspection.setSeverityLevel(VillaInspection.SeverityLevel.NORMAL);
            reinspection.setNotes("Tái kiểm định nghiệm thu chất lượng sau hoàn tất bảo trì #" + maintenanceId + ": " + (m.getReason() != null ? m.getReason() : ""));
            reinspection.setScheduledDate(LocalDateTime.now());
            inspectionRepository.save(reinspection);
        }

        return saved;
    }

    @Transactional
    public void cancelMaintenance(Integer maintenanceId) {
        VillaMaintenance m = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("VillaMaintenance", maintenanceId));
        m.setStatus(MaintenanceStatus.CANCELLED);
        maintenanceRepository.save(m);
    }
}
