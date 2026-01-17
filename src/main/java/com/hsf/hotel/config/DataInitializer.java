package com.hsf.hotel.config;

import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.RoomType;
import com.hsf.hotel.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoomRepository roomRepository;

    @Override
    public void run(String... args) {
        // Only initialize if no rooms exist
        if (roomRepository.count() == 0) {
            initializeRooms();
        }
    }

    private void initializeRooms() {
        // Standard Rooms
        createRoom("101", RoomType.STANDARD, new BigDecimal("500000"),
                "Phòng đơn yên tĩnh, đầy đủ tiện nghi cơ bản. Thích hợp cho khách đi công tác.", null);
        createRoom("102", RoomType.STANDARD, new BigDecimal("500000"),
                "Phòng đơn view sân vườn, không gian thoáng mát.", null);
        createRoom("103", RoomType.STANDARD, new BigDecimal("550000"),
                "Phòng đôi tiêu chuẩn, phù hợp cho cặp đôi.", null);

        // Deluxe Rooms
        createRoom("201", RoomType.DELUXE, new BigDecimal("1200000"),
                "Phòng Deluxe view biển, có ban công riêng và bồn tắm.", null);
        createRoom("202", RoomType.DELUXE, new BigDecimal("1200000"),
                "Phòng Deluxe cao cấp với nội thất sang trọng, view thành phố.", null);
        createRoom("203", RoomType.DELUXE, new BigDecimal("1350000"),
                "Phòng Deluxe Family, rộng rãi cho gia đình 4 người.", null);

        // Suite Rooms
        createRoom("301", RoomType.SUITE, new BigDecimal("2500000"),
                "Suite sang trọng với phòng khách riêng, jacuzzi và minibar.", null);
        createRoom("302", RoomType.SUITE, new BigDecimal("2800000"),
                "Suite Executive với không gian làm việc riêng và view panorama.", null);

        // VIP Rooms
        createRoom("P01", RoomType.VIP, new BigDecimal("5000000"),
                "Penthouse VIP với sân thượng riêng, bể bơi mini và dịch vụ butler 24/7.", null);
        createRoom("P02", RoomType.VIP, new BigDecimal("6500000"),
                "Royal Suite - Căn hộ cao cấp nhất với 2 phòng ngủ, phòng khách rộng và tầm nhìn 360 độ.", null);

        System.out.println("✅ Đã khởi tạo " + roomRepository.count() + " phòng mẫu!");
    }

    private void createRoom(String roomNumber, RoomType type, BigDecimal price, String description, String imageUrl) {
        Room room = new Room();
        room.setRoomNumber(roomNumber);
        room.setRoomType(type);
        room.setPricePerNight(price);
        room.setDescription(description);
        room.setImageUrl(imageUrl);
        room.setIsAvailable(true);
        roomRepository.save(room);
    }
}
