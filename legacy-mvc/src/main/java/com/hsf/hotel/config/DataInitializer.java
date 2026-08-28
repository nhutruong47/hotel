package com.hsf.hotel.config;

import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.RoomTypeEntity;
import com.hsf.hotel.model.User;
import com.hsf.hotel.repository.RoomRepository;
import com.hsf.hotel.repository.RoomTypeRepository;
import com.hsf.hotel.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

        @Autowired
        private RoomRepository roomRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RoomTypeRepository roomTypeRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Override
        public void run(String... args) {
                // Create default user if not exists
                if (userRepository.findByUsername("a").isEmpty()) {
                        User user = new User();
                        user.setUsername("a");
                        user.setPassword(passwordEncoder.encode("a"));
                        user.setFullName("Admin User");
                        user.setRole("ADMIN");
                        userRepository.save(user);
                        System.out.println("✅ Đã tạo user mặc định: username=a, password=a");
                }

                // Create or update admin account
                userRepository.findByUsername("admin").ifPresentOrElse(
                                existingAdmin -> {
                                        existingAdmin.setPassword(passwordEncoder.encode("admin"));
                                        existingAdmin.setRole("ADMIN");
                                        userRepository.save(existingAdmin);
                                        System.out.println(
                                                        "✅ Đã cập nhật tài khoản admin: username=admin, password=admin");
                                },
                                () -> {
                                        User admin = new User();
                                        admin.setUsername("admin");
                                        admin.setPassword(passwordEncoder.encode("admin"));
                                        admin.setFullName("Administrator");
                                        admin.setEmail("admin@hotel.com");
                                        admin.setRole("ADMIN");
                                        userRepository.save(admin);
                                        System.out.println("✅ Đã tạo tài khoản admin: username=admin, password=admin");
                                });

                // Always ensure default room types exist
                ensureRoomTypesExist();

                // Repair rooms that reference non-existent room types (fixes data integrity)
                repairOrphanedRooms();

                // Only initialize rooms if no rooms exist
                if (roomRepository.count() == 0) {
                        initializeRooms();
                }
        }

        private void ensureRoomTypesExist() {
                String[][] defaultTypes = {
                                { "STANDARD", "Phòng tiêu chuẩn" },
                                { "DELUXE", "Phòng cao cấp" },
                                { "SUITE", "Phòng suite" },
                                { "VIP", "Phòng VIP" }
                };

                for (String[] type : defaultTypes) {
                        roomTypeRepository.findByName(type[0]).orElseGet(() -> {
                                RoomTypeEntity newType = new RoomTypeEntity(type[0], type[1]);
                                System.out.println("✅ Đã tạo loại phòng: " + type[0]);
                                return roomTypeRepository.save(newType);
                        });
                }
        }

        private void repairOrphanedRooms() {
                // Use native SQL to fix rooms referencing non-existent room types
                // This must run BEFORE any JPA findAll() on Rooms to avoid
                // EntityNotFoundException
                RoomTypeEntity defaultType = roomTypeRepository.findByName("STANDARD").orElse(null);
                if (defaultType == null) {
                        return;
                }

                int updated = jdbcTemplate.update(
                                "UPDATE rooms SET room_type_id = ? WHERE room_type_id NOT IN (SELECT id FROM room_types)",
                                defaultType.getId());

                if (updated > 0) {
                        System.out.println("⚠️ Đã sửa " + updated + " phòng có loại phòng không hợp lệ → STANDARD");
                }
        }

        private void initializeRooms() {
                // Standard Rooms
                createRoom("101", "STANDARD", new BigDecimal("500000"),
                                "Phòng đơn yên tĩnh, đầy đủ tiện nghi cơ bản. Thích hợp cho khách đi công tác.",
                                "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=400");
                createRoom("102", "STANDARD", new BigDecimal("500000"),
                                "Phòng đơn view sân vườn, không gian thoáng mát.",
                                "https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=400");
                createRoom("103", "STANDARD", new BigDecimal("550000"),
                                "Phòng đôi tiêu chuẩn, phù hợp cho cặp đôi.",
                                "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=400");

                // Deluxe Rooms
                createRoom("201", "DELUXE", new BigDecimal("1200000"),
                                "Phòng Deluxe view biển, có ban công riêng và bồn tắm.",
                                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=400");
                createRoom("202", "DELUXE", new BigDecimal("1200000"),
                                "Phòng Deluxe cao cấp với nội thất sang trọng, view thành phố.",
                                "https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=400");
                createRoom("203", "DELUXE", new BigDecimal("1350000"),
                                "Phòng Deluxe Family, rộng rãi cho gia đình 4 người.",
                                "https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=400");

                // Suite Rooms
                createRoom("301", "SUITE", new BigDecimal("2500000"),
                                "Suite sang trọng với phòng khách riêng, jacuzzi và minibar.",
                                "https://images.unsplash.com/photo-1591088398332-8a7791972843?w=400");
                createRoom("302", "SUITE", new BigDecimal("2800000"),
                                "Suite Executive với không gian làm việc riêng và view panorama.",
                                "https://images.unsplash.com/photo-1596394516093-501ba68a0ba6?w=400");

                // VIP Rooms
                createRoom("P01", "VIP", new BigDecimal("5000000"),
                                "Penthouse VIP với sân thượng riêng, bể bơi mini và dịch vụ butler 24/7.",
                                "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=400");
                createRoom("P02", "VIP", new BigDecimal("6500000"),
                                "Royal Suite - Căn hộ cao cấp nhất với 2 phòng ngủ, phòng khách rộng và tầm nhìn 360 độ.",
                                "https://images.unsplash.com/photo-1602002418816-5c0aeef426aa?w=400");

                System.out.println("✅ Đã khởi tạo " + roomRepository.count() + " phòng mẫu!");
        }

        private void createRoom(String roomNumber, String typeName, BigDecimal price, String description,
                        String imageUrl) {

                RoomTypeEntity roomType = roomTypeRepository.findByName(typeName).orElseGet(() -> {
                        RoomTypeEntity newType = new RoomTypeEntity(typeName, typeName);
                        return roomTypeRepository.save(newType);
                });

                Room room = new Room();
                room.setRoomNumber(roomNumber);
                room.setRoomType(roomType);
                room.setPricePerNight(price);
                room.setDescription(description);
                room.setImageUrl(imageUrl);
                room.setIsAvailable(true);
                roomRepository.save(room);
        }
}
