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
                        System.out.println("✅ Default user created: username=a, password=a");
                }

                // Create or update admin account
                userRepository.findByUsername("admin").ifPresentOrElse(
                                existingAdmin -> {
                                        existingAdmin.setPassword(passwordEncoder.encode("admin"));
                                        existingAdmin.setRole("ADMIN");
                                        userRepository.save(existingAdmin);
                                        System.out.println(
                                                        "✅ Admin account updated: username=admin, password=admin");
                                },
                                () -> {
                                        User admin = new User();
                                        admin.setUsername("admin");
                                        admin.setPassword(passwordEncoder.encode("admin"));
                                        admin.setFullName("Administrator");
                                        admin.setEmail("admin@hotel.com");
                                        admin.setRole("ADMIN");
                                        userRepository.save(admin);
                                        System.out.println("✅ Admin account created: username=admin, password=admin");
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
                                { "STANDARD", "Standard Room" },
                                { "DELUXE", "Deluxe Room" },
                                { "SUITE", "Suite Room" },
                                { "VIP", "VIP Room" }
                };

                for (String[] type : defaultTypes) {
                        roomTypeRepository.findByName(type[0]).orElseGet(() -> {
                                RoomTypeEntity newType = new RoomTypeEntity(type[0], type[1]);
                                System.out.println("✅ Created room type: " + type[0]);
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
                                "UPDATE Rooms SET room_type_id = ? WHERE room_type_id NOT IN (SELECT id FROM RoomTypes)",
                                defaultType.getId());

                if (updated > 0) {
                        System.out.println("⚠️ Fixed " + updated + " rooms with invalid room types → STANDARD");
                }
        }

        private void initializeRooms() {
                // Standard Rooms
                createRoom("101", "STANDARD", new BigDecimal("500000"),
                                "Quiet single room with basic amenities. Ideal for business travelers.",
                                "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=400");
                createRoom("102", "STANDARD", new BigDecimal("500000"),
                                "Single room with garden view, airy space.",
                                "https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=400");
                createRoom("103", "STANDARD", new BigDecimal("550000"),
                                "Standard double room, suitable for couples.",
                                "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=400");

                // Deluxe Rooms
                createRoom("201", "DELUXE", new BigDecimal("1200000"),
                                "Deluxe room with sea view, private balcony, and bathtub.",
                                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=400");
                createRoom("202", "DELUXE", new BigDecimal("1200000"),
                                "Premium Deluxe room with luxurious interior, city view.",
                                "https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=400");
                createRoom("203", "DELUXE", new BigDecimal("1350000"),
                                "Deluxe Family room, spacious for a family of 4.",
                                "https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=400");

                // Suite Rooms
                createRoom("301", "SUITE", new BigDecimal("2500000"),
                                "Luxury Suite with private living room, jacuzzi, and minibar.",
                                "https://images.unsplash.com/photo-1591088398332-8a7791972843?w=400");
                createRoom("302", "SUITE", new BigDecimal("2800000"),
                                "Executive Suite with private workspace and panoramic view.",
                                "https://images.unsplash.com/photo-1596394516093-501ba68a0ba6?w=400");

                // VIP Rooms
                createRoom("P01", "VIP", new BigDecimal("5000000"),
                                "VIP Penthouse with private terrace, mini-pool, and 24/7 butler service.",
                                "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=400");
                createRoom("P02", "VIP", new BigDecimal("6500000"),
                                "Royal Suite - The most premium apartment with 2 bedrooms, spacious living room, and 360-degree view.",
                                "https://images.unsplash.com/photo-1602002418816-5c0aeef426aa?w=400");

                System.out.println("✅ Initialized " + roomRepository.count() + " sample rooms!");
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
