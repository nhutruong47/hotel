package com.hsf.hotel.config;

import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.RoomType;
import com.hsf.hotel.model.User;
import com.hsf.hotel.repository.RoomRepository;
import com.hsf.hotel.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

        @Autowired
        private RoomRepository roomRepository;

        @Autowired
        private UserRepository userRepository;

        @Override
        public void run(String... args) {
                // Create default user if not exists
                if (userRepository.findByUsername("a").isEmpty()) {
                        User user = new User();
                        user.setUsername("a");
                        user.setPassword("a");
                        user.setFullName("Admin User");
                        user.setRole("ADMIN");
                        userRepository.save(user);
                        System.out.println("✅ Đã tạo user mặc định: username=a, password=a");
                }

                // Create or update admin account
                userRepository.findByUsername("admin").ifPresentOrElse(
                                existingAdmin -> {
                                        existingAdmin.setPassword("admin");
                                        existingAdmin.setRole("ADMIN");
                                        userRepository.save(existingAdmin);
                                        System.out.println(
                                                        "✅ Đã cập nhật tài khoản admin: username=admin, password=admin");
                                },
                                () -> {
                                        User admin = new User();
                                        admin.setUsername("admin");
                                        admin.setPassword("admin");
                                        admin.setFullName("Administrator");
                                        admin.setEmail("admin@hotel.com");
                                        admin.setRole("ADMIN");
                                        userRepository.save(admin);
                                        System.out.println("✅ Đã tạo tài khoản admin: username=admin, password=admin");
                                });

                // Only initialize rooms if no rooms exist
                if (roomRepository.count() == 0) {
                        initializeRooms();
                }
        }

        private void initializeRooms() {
                // Standard Rooms
                createRoom("101", RoomType.STANDARD, new BigDecimal("500000"),
                                "Phòng đơn yên tĩnh, đầy đủ tiện nghi cơ bản. Thích hợp cho khách đi công tác.",
                                "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=400");
                createRoom("102", RoomType.STANDARD, new BigDecimal("500000"),
                                "Phòng đơn view sân vườn, không gian thoáng mát.",
                                "https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=400");
                createRoom("103", RoomType.STANDARD, new BigDecimal("550000"),
                                "Phòng đôi tiêu chuẩn, phù hợp cho cặp đôi.",
                                "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=400");

                // Deluxe Rooms
                createRoom("201", RoomType.DELUXE, new BigDecimal("1200000"),
                                "Phòng Deluxe view biển, có ban công riêng và bồn tắm.",
                                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=400");
                createRoom("202", RoomType.DELUXE, new BigDecimal("1200000"),
                                "Phòng Deluxe cao cấp với nội thất sang trọng, view thành phố.",
                                "https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=400");
                createRoom("203", RoomType.DELUXE, new BigDecimal("1350000"),
                                "Phòng Deluxe Family, rộng rãi cho gia đình 4 người.",
                                "https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=400");

                // Suite Rooms
                createRoom("301", RoomType.SUITE, new BigDecimal("2500000"),
                                "Suite sang trọng với phòng khách riêng, jacuzzi và minibar.",
                                "https://images.unsplash.com/photo-1591088398332-8a7791972843?w=400");
                createRoom("302", RoomType.SUITE, new BigDecimal("2800000"),
                                "Suite Executive với không gian làm việc riêng và view panorama.",
                                "https://images.unsplash.com/photo-1596394516093-501ba68a0ba6?w=400");

                // VIP Rooms
                createRoom("P01", RoomType.VIP, new BigDecimal("5000000"),
                                "Penthouse VIP với sân thượng riêng, bể bơi mini và dịch vụ butler 24/7.",
                                "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=400");
                createRoom("P02", RoomType.VIP, new BigDecimal("6500000"),
                                "Royal Suite - Căn hộ cao cấp nhất với 2 phòng ngủ, phòng khách rộng và tầm nhìn 360 độ.",
                                "https://images.unsplash.com/photo-1602002418816-5c0aeef426aa?w=400");

                System.out.println("✅ Đã khởi tạo " + roomRepository.count() + " phòng mẫu!");
        }

        private void createRoom(String roomNumber, RoomType type, BigDecimal price, String description,
                        String imageUrl) {
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
