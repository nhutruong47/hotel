package com.hsf.hotel.room.service;

import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.VillaInspection;
import com.hsf.hotel.room.model.VillaMaintenance;
import com.hsf.hotel.room.model.VillaMaintenance.MaintenanceStatus;
import com.hsf.hotel.room.repository.RoomRepository;
import com.hsf.hotel.room.repository.VillaInspectionRepository;
import com.hsf.hotel.room.repository.VillaMaintenanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VillaMaintenanceServiceTest {

    @Mock
    private VillaMaintenanceRepository maintenanceRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private VillaInspectionRepository inspectionRepository;

    @InjectMocks
    private VillaMaintenanceService maintenanceService;

    private Room testRoom;
    private VillaMaintenance testMaintenance;

    @BeforeEach
    void setUp() {
        testRoom = new Room();
        testRoom.setId(5);
        testRoom.setRoomNumber("VILLA-105");

        testMaintenance = new VillaMaintenance(testRoom, LocalDate.now(), LocalDate.now().plusDays(2), "Fix HVAC", null);
        testMaintenance.setId(12);
        testMaintenance.setStatus(MaintenanceStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should auto-schedule re-inspection when maintenance is updated to COMPLETED")
    void testAutoScheduleReinspectionOnCompleted() {
        when(maintenanceRepository.findById(12)).thenReturn(Optional.of(testMaintenance));
        when(maintenanceRepository.save(any(VillaMaintenance.class))).thenReturn(testMaintenance);

        VillaMaintenance result = maintenanceService.updateStatus(12, MaintenanceStatus.COMPLETED);

        assertEquals(MaintenanceStatus.COMPLETED, result.getStatus());

        ArgumentCaptor<VillaInspection> captor = ArgumentCaptor.forClass(VillaInspection.class);
        verify(inspectionRepository).save(captor.capture());

        VillaInspection savedInspection = captor.getValue();
        assertEquals(testRoom, savedInspection.getRoom());
        assertEquals(VillaInspection.InspectionStatus.SCHEDULED, savedInspection.getStatus());
        assertEquals(VillaInspection.InspectionType.ROUTINE_CHECK, savedInspection.getInspectionType());
        assertNotNull(savedInspection.getNotes());
    }

    @Test
    @DisplayName("Should not schedule re-inspection if maintenance is already COMPLETED")
    void testNoDuplicateReinspectionIfAlreadyCompleted() {
        testMaintenance.setStatus(MaintenanceStatus.COMPLETED);
        when(maintenanceRepository.findById(12)).thenReturn(Optional.of(testMaintenance));
        when(maintenanceRepository.save(any(VillaMaintenance.class))).thenReturn(testMaintenance);

        maintenanceService.updateStatus(12, MaintenanceStatus.COMPLETED);

        verify(inspectionRepository, never()).save(any());
    }
}
