package com.hsf.hotel.config;

import com.hsf.hotel.faq.model.FaqItem;
import com.hsf.hotel.faq.model.FaqItem.FaqCategory;
import com.hsf.hotel.faq.repository.FaqItemRepository;
import com.hsf.hotel.promotion.model.Promotion;
import com.hsf.hotel.promotion.model.Promotion.PromotionCategory;
import com.hsf.hotel.promotion.repository.PromotionRepository;
import com.hsf.hotel.review.model.Review;
import com.hsf.hotel.review.repository.ReviewRepository;
import com.hsf.hotel.room.model.Amenity;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.RoomTypeEntity;
import com.hsf.hotel.room.repository.AmenityRepository;
import com.hsf.hotel.room.repository.RoomRepository;
import com.hsf.hotel.room.repository.RoomTypeRepository;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.model.UserRole;
import com.hsf.hotel.user.repository.UserRepository;
import com.hsf.hotel.voucher.model.Voucher;
import com.hsf.hotel.voucher.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds comprehensive luxury villa data, gallery photos, coordinates, FAQs,
 * promotions, vouchers, and verified guest reviews for Nhu Villas.
 */
@Component
@Profile({"default", "dev", "postgres", "seed"})
@Lazy(false)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final FaqItemRepository faqRepository;
    private final PromotionRepository promotionRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final AmenityRepository amenityRepository;
    private final VoucherRepository voucherRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initializeFaqs();
        initializeRoomsAndEnrich();
        ensureRoomSlugs();
        initializePromotions();
        initializeVouchers();
        initializeSampleReviews();
    }

    private RoomTypeEntity getOrCreateRoomType(String name, String description) {
        return roomTypeRepository.findByName(name).orElseGet(() -> {
            RoomTypeEntity rt = new RoomTypeEntity();
            rt.setName(name);
            rt.setDescription(description);
            return roomTypeRepository.save(rt);
        });
    }

    private Amenity getOrCreateAmenity(String name) {
        return amenityRepository.findByName(name).orElseGet(() -> {
            Amenity a = new Amenity();
            a.setName(name);
            return amenityRepository.save(a);
        });
    }

    private void initializeRoomsAndEnrich() {
        RoomTypeEntity poolVilla = getOrCreateRoomType("Pool Villa", "A one-bedroom garden villa with a private heated pool.");
        RoomTypeEntity familyVilla = getOrCreateRoomType("Family Villa", "Two quiet bedrooms, generous living space, and private terrace.");
        RoomTypeEntity residence = getOrCreateRoomType("Residence", "A larger private residence for hosted dinners and hillside sunsets.");
        RoomTypeEntity signature = getOrCreateRoomType("Signature", "The flagship four-bedroom estate in our collection.");
        RoomTypeEntity pavilion = getOrCreateRoomType("Pavilion", "An intimate forest pavilion surrounded by pine trees and mist.");

        Amenity pool = getOrCreateAmenity("Private pool");
        Amenity infinityPool = getOrCreateAmenity("Infinity pool");
        Amenity breakfast = getOrCreateAmenity("Breakfast");
        Amenity wifi = getOrCreateAmenity("Wi-Fi");
        Amenity garden = getOrCreateAmenity("Garden");
        Amenity terrace = getOrCreateAmenity("Terrace");
        Amenity concierge = getOrCreateAmenity("Concierge");
        Amenity transfer = getOrCreateAmenity("Airport transfer");
        Amenity spa = getOrCreateAmenity("Spa");
        Amenity kitchen = getOrCreateAmenity("Kitchen");

        upsertVilla(
            "GV-01",
            "garden-pool-villa-da-lat",
            poolVilla,
            BigDecimal.valueOf(450),
            2, 1, true,
            BigDecimal.valueOf(4.9), 38L,
            "A one-bedroom garden villa with a private heated pool, shaded stone terrace, and open-air living room overlooking pine valleys.",
            "/images/nhu-garden-pool-villa.jpg",
            List.of(
                "/images/nhu-garden-pool-villa.jpg",
                "/images/nhu-villa-interior.jpg",
                "/images/nhu-infinity-pool.jpg",
                "/images/nhu-private-dining.jpg",
                "/images/nhu-location-landscape.jpg"
            ),
            List.of(pool, breakfast, wifi, garden, terrace),
            11.9038, 108.4368,
            "30 phút từ Sân bay Liên Khương (DLI) — có xe Mercedes V-Class đưa đón riêng",
            "Hồ Tuyền Lâm (5 phút), Thiền Viện Trúc Lâm (8 phút), Đồi thông Pinhatt (12 phút)",
            "Nhà hàng Le Chalet Dalat, Émai Italian Dining, Chef's Table tại Villa",
            "In The Forest Café, Khu vườn Mơ, Phê La Đà Lạt"
        );

        upsertVilla(
            "TF-02",
            "two-bedroom-family-villa",
            familyVilla,
            BigDecimal.valueOf(720),
            4, 2, true,
            BigDecimal.valueOf(4.85), 27L,
            "Two quiet bedrooms, generous timber-and-linen living space, and a private courtyard terrace prepared for slow family mornings.",
            "/images/nhu-villa-interior.jpg",
            List.of(
                "/images/nhu-villa-interior.jpg",
                "/images/nhu-garden-pool-villa.jpg",
                "/images/nhu-private-dining.jpg",
                "/images/nhu-infinity-pool.jpg",
                "/images/nhu-hero-villa.jpg"
            ),
            List.of(pool, breakfast, wifi, kitchen, garden, terrace),
            11.9045, 108.4375,
            "30 phút từ Sân bay Liên Khương (DLI)",
            "Hồ Tuyền Lâm, Đường hầm Điêu khắc, Rừng thông cổ thụ",
            "Biệt thự ẩm thực Nhu Dining, Primavera Italian Restaurant",
            "Tiệm cà phê Túi Mơ To, Horizon Pine View"
        );

        upsertVilla(
            "HR-03",
            "hillside-residence-private-pool",
            residence,
            BigDecimal.valueOf(1150),
            6, 3, true,
            BigDecimal.valueOf(4.95), 19L,
            "A larger private hillside residence designed for hosted chef dinners, longer retreats, and unforgettable sunset infinity-pool rituals.",
            "/images/nhu-infinity-pool.jpg",
            List.of(
                "/images/nhu-infinity-pool.jpg",
                "/images/nhu-private-dining.jpg",
                "/images/nhu-villa-interior.jpg",
                "/images/nhu-hero-villa-4k.jpg",
                "/images/nhu-location-landscape.jpg"
            ),
            List.of(infinityPool, breakfast, wifi, concierge, transfer, kitchen, spa),
            11.9022, 108.4391,
            "35 phút từ Sân bay Liên Khương (DLI) — Miễn phí đưa đón khứ hồi",
            "Đỉnh Pinhatt ngắm bình minh, Bến du thuyền Hồ Tuyền Lâm, Sân Golf Sacom",
            "Private Butler & Chef's Table phục vụ tại Residence",
            "Private Wine Cellar & Tea Lounge tại Residence"
        );

        upsertVilla(
            "SE-04",
            "signature-four-bedroom-estate",
            signature,
            BigDecimal.valueOf(2500),
            8, 4, true,
            BigDecimal.valueOf(5.0), 12L,
            "The largest estate in our collection, offering panoramic lake and pine forest views, four master suites, private spa pavilion, and 24/7 butler service.",
            "/images/nhu-private-dining.jpg",
            List.of(
                "/images/nhu-private-dining.jpg",
                "/images/nhu-hero-villa-4k.jpg",
                "/images/nhu-infinity-pool.jpg",
                "/images/nhu-garden-pool-villa.jpg",
                "/images/nhu-villa-interior.jpg"
            ),
            List.of(infinityPool, pool, breakfast, wifi, concierge, transfer, spa, kitchen, garden, terrace),
            11.9010, 108.4405,
            "35 phút từ Sân bay Liên Khương (DLI) — Đưa đón bằng Limousine riêng",
            "Bến thuyền riêng Hồ Tuyền Lâm, Sân Golf 18 lỗ, Khu bảo tồn rừng thông",
            "Đầu bếp riêng 5 sao phục vụ thực đơn Fine Dining theo yêu cầu",
            "Phòng thưởng trà & hầm rượu vang riêng trong khuôn viên Estate"
        );

        upsertVilla(
            "WC-05",
            "water-courtyard-villa",
            poolVilla,
            BigDecimal.valueOf(560),
            2, 1, true,
            BigDecimal.valueOf(4.9), 31L,
            "A tranquil courtyard villa arranged around reflective water features, natural stone, organic linen, and sunlit spaces for late breakfast rituals.",
            "/images/nhu-hero-villa.jpg",
            List.of(
                "/images/nhu-hero-villa.jpg",
                "/images/nhu-garden-pool-villa.jpg",
                "/images/nhu-villa-interior.jpg",
                "/images/nhu-private-dining.jpg",
                "/images/nhu-location-landscape.jpg"
            ),
            List.of(pool, breakfast, wifi, spa, terrace),
            11.9032, 108.4359,
            "30 phút từ Sân bay Liên Khương (DLI)",
            "Hồ Tuyền Lâm, Thiền Viện Trúc Lâm, Thác Datanla",
            "Nhu Courtyard Dining, Nhà hàng Memory Dalat",
            "La Viet Coffee, Rừng Thông Mơ Bistro"
        );

        upsertVilla(
            "FP-06",
            "forest-pavilion",
            pavilion,
            BigDecimal.valueOf(390),
            2, 1, true,
            BigDecimal.valueOf(4.8), 24L,
            "An intimate architectural pavilion crafted for couples seeking deep pine shade, the natural sound of mountain streams, and complete seclusion.",
            "/images/nhu-location-landscape.jpg",
            List.of(
                "/images/nhu-location-landscape.jpg",
                "/images/nhu-villa-interior.jpg",
                "/images/nhu-garden-pool-villa.jpg",
                "/images/nhu-infinity-pool.jpg",
                "/images/nhu-private-dining.jpg"
            ),
            List.of(breakfast, wifi, garden, terrace, spa),
            11.9051, 108.4342,
            "30 phút từ Sân bay Liên Khương (DLI)",
            "Đường mòn trekking rừng thông, Hồ Tuyền Lâm, Đồi cỏ hồng",
            "Thực đơn Organic Farm-to-Table phục vụ tại hiên gỗ",
            "Trà chiều thảo mộc Đà Lạt tại Pavilion"
        );
    }

    private void upsertVilla(
            String roomNumber,
            String slug,
            RoomTypeEntity roomType,
            BigDecimal price,
            int capacity,
            int bedrooms,
            boolean available,
            BigDecimal avgRating,
            long reviewCount,
            String description,
            String imageUrl,
            List<String> gallery,
            List<Amenity> amenities,
            double lat,
            double lng,
            String airport,
            String attractions,
            String restaurants,
            String cafes
    ) {
        Room room = roomRepository.findByRoomNumber(roomNumber).orElseGet(Room::new);
        room.setRoomNumber(roomNumber);
        room.setSlug(slug);
        room.setRoomType(roomType);
        room.setPricePerNight(price);
        room.setCapacity(capacity);
        room.setBedrooms(bedrooms);
        if (room.getId() == null) {
            room.setIsAvailable(available);
            room.setAvgRating(avgRating);
            room.setReviewCount(reviewCount);
        }
        room.setDescription(description);
        room.setImageUrl(imageUrl);
        room.getGalleryImages().clear();
        room.getGalleryImages().addAll(gallery);
        room.getAmenities().clear();
        room.getAmenities().addAll(amenities);
        room.setLatitude(lat);
        room.setLongitude(lng);
        room.setNearbyAirport(airport);
        room.setNearbyAttractions(attractions);
        room.setNearbyRestaurants(restaurants);
        room.setNearbyCafes(cafes);
        room.setCheckInTime("14:00");
        room.setCheckOutTime("12:00");
        room.setHouseRules("Không hút thuốc trong phòng kín · Giữ không gian yên tĩnh sau 22:00 · Phù hợp cho nghỉ dưỡng riêng tư.");
        room.setPolicies("Miễn phí hủy phòng trước 3 ngày nhận phòng · Hoàn 50% từ 1-3 ngày · Hỗ trợ đổi lịch linh hoạt.");
        roomRepository.save(room);
    }

    private void ensureRoomSlugs() {
        for (Room r : roomRepository.findAll()) {
            if (r.getSlug() == null || r.getSlug().isBlank()) {
                if ("GV-01".equalsIgnoreCase(r.getRoomNumber())) r.setSlug("garden-pool-villa-da-lat");
                else if ("TF-02".equalsIgnoreCase(r.getRoomNumber())) r.setSlug("two-bedroom-family-villa");
                else if ("HR-03".equalsIgnoreCase(r.getRoomNumber())) r.setSlug("hillside-residence-private-pool");
                else if ("SE-04".equalsIgnoreCase(r.getRoomNumber())) r.setSlug("signature-four-bedroom-estate");
                else if ("WC-05".equalsIgnoreCase(r.getRoomNumber())) r.setSlug("water-courtyard-villa");
                else if ("FP-06".equalsIgnoreCase(r.getRoomNumber())) r.setSlug("forest-pavilion");
                else r.setSlug("villa-" + r.getRoomNumber().toLowerCase().replace(" ", "-"));
                roomRepository.save(r);
            }
        }
    }

    private void initializeFaqs() {
        if (faqRepository.count() > 0) return;

        faqRepository.save(new FaqItem(
            "Làm thế nào để đặt biệt thự tại Nhu Villas?",
            "Quý khách có thể đặt trực tiếp trên website bằng cách chọn biệt thự, ngày nhận/trả phòng và phương thức thanh toán (Thẻ quốc tế hoặc Chuyển khoản). Đội ngũ Quản gia sẽ gửi xác nhận ngay lập tức.",
            FaqCategory.BOOKING
        ));
        faqRepository.save(new FaqItem(
            "Chính sách hủy và đổi lịch lưu trú như thế nào?",
            "Quý khách được hoàn tiền 100% khi hủy trước 3 ngày so với ngày check-in, hoàn 50% khi hủy từ 1–3 ngày trước check-in, và hỗ trợ đổi ngày lưu trú miễn phí 1 lần tùy theo tình trạng phòng.",
            FaqCategory.CANCELLATION
        ));
        faqRepository.save(new FaqItem(
            "Phương thức thanh toán nào được chấp nhận?",
            "Chúng tôi chấp nhận thẻ tín dụng/ghi nợ quốc tế (Visa, Mastercard, Amex qua Stripe), chuyển khoản ngân hàng VietQR 24/7 và thanh toán trực tiếp tại khu nghỉ dưỡng.",
            FaqCategory.PAYMENT
        ));
        faqRepository.save(new FaqItem(
            "Quy định về thời gian hoàn tiền?",
            "Yêu cầu hoàn tiền được xử lý tự động trong vòng 3–7 ngày làm việc đối với thẻ quốc tế và trong 24 giờ đối với chuyển khoản ngân hàng nội địa.",
            FaqCategory.REFUND
        ));
        faqRepository.save(new FaqItem(
            "Giờ nhận phòng (Check-in) và trả phòng (Check-out)?",
            "Giờ nhận phòng tiêu chuẩn là 14:00 và giờ trả phòng là 12:00 trưa. Quý khách có thể yêu cầu nhận phòng sớm hoặc trả phòng muộn miễn phí tùy thuộc vào lịch trống của biệt thự.",
            FaqCategory.POLICIES
        ));
        faqRepository.save(new FaqItem(
            "Giá biệt thự đã bao gồm bữa sáng và quản gia chưa?",
            "Tất cả các kỳ nghỉ tại Nhu Villas đều đã bao gồm bữa sáng thực đơn À-la-carte phục vụ tại biệt thự, trà chiều Đà Lạt, minibar thủ công và dịch vụ Quản gia chăm sóc 24/7.",
            FaqCategory.SERVICES
        ));
        faqRepository.save(new FaqItem(
            "Hồ bơi riêng tại biệt thự có hệ thống nước ấm không?",
            "Toàn bộ hồ bơi riêng và hồ bơi vô cực tại Nhu Villas đều được trang bị hệ thống gia nhiệt nước ấm tự động (28–31°C), giúp quý khách thoải mái bơi lội giữa tiết trời se lạnh của Đà Lạt.",
            FaqCategory.FACILITIES
        ));
        faqRepository.save(new FaqItem(
            "Khu nghỉ dưỡng có dịch vụ đưa đón sân bay Liên Khương không?",
            "Chúng tôi cung cấp dịch vụ xe riêng đưa đón từ Sân bay Liên Khương (chỉ 30 phút di chuyển). Các hạng Residence và Signature Estate được miễn phí hoàn toàn dịch vụ đưa đón khứ hồi.",
            FaqCategory.TRANSPORT
        ));
    }

    private void initializePromotions() {
        List<Room> rooms = roomRepository.findAll();
        Room r1 = rooms.stream().filter(r -> "GV-01".equals(r.getRoomNumber())).findFirst().orElse(null);
        Room r2 = rooms.stream().filter(r -> "TF-02".equals(r.getRoomNumber())).findFirst().orElse(null);
        Room r3 = rooms.stream().filter(r -> "HR-03".equals(r.getRoomNumber())).findFirst().orElse(null);
        Room r4 = rooms.stream().filter(r -> "SE-04".equals(r.getRoomNumber())).findFirst().orElse(null);
        Room r5 = rooms.stream().filter(r -> "WC-05".equals(r.getRoomNumber())).findFirst().orElse(null);
        Room r6 = rooms.stream().filter(r -> "FP-06".equals(r.getRoomNumber())).findFirst().orElse(null);

        if (promotionRepository.count() > 0) {
            // Ensure promoCodes exist on all promotions so users can apply them at checkout
            for (Promotion p : promotionRepository.findAll()) {
                if (p.getPromoCode() == null || p.getPromoCode().isBlank()) {
                    if ("Summer Escape".equalsIgnoreCase(p.getTitle())) p.setPromoCode("SUMMER15");
                    else if ("Honeymoon Special".equalsIgnoreCase(p.getTitle())) p.setPromoCode("HONEYMOON20");
                    else p.setPromoCode("NHUVIP15");
                    promotionRepository.save(p);
                }
            }
            return;
        }

        LocalDate today = LocalDate.now();

        Promotion p1 = new Promotion();
        p1.setTitle("Summer Escape");
        p1.setSubtitle("Giảm 15% cho kỳ nghỉ mùa hè");
        p1.setDescription("Tận hưởng không khí trong lành tại rừng thông Đà Lạt với ưu đãi giảm 15% cho kỳ nghỉ từ 2 đêm trở lên.");
        p1.setTermsConditions("Áp dụng cho đặt phòng từ 2 đêm. Bao gồm bữa sáng và trà chiều tại biệt thự.");
        p1.setCategory(PromotionCategory.SEASONAL);
        if (r3 != null) p1.getRooms().add(r3);
        if (r5 != null) p1.getRooms().add(r5);
        p1.setIsActive(true);
        p1.setIsFeatured(true);
        p1.setStartDate(today.minusDays(5));
        p1.setEndDate(today.plusMonths(3));
        p1.setCountdownEndDate(LocalDateTime.now().plusDays(14));
        p1.setDiscountPercent(BigDecimal.valueOf(15));
        p1.setMinimumNights(2);
        p1.setMinimumBookingAmount(BigDecimal.valueOf(300));
        p1.setPromoCode("SUMMER15");
        p1.setImageUrl("/images/nhu-infinity-pool.jpg");
        p1.setDisplayOrder(1);
        promotionRepository.save(p1);

        Promotion p2 = new Promotion();
        p2.setTitle("Weekend Retreat");
        p2.setSubtitle("Cuối tuần thư thái");
        p2.setDescription("Ưu đãi 10% cho mọi kỳ nghỉ cuối tuần tại toàn bộ hệ thống biệt thự Nhu Villas.");
        p2.setTermsConditions("Áp dụng cho mọi hạng biệt thự, tối thiểu 1 đêm.");
        p2.setCategory(PromotionCategory.WEEKEND);
        p2.setIsActive(true);
        p2.setIsFeatured(false);
        p2.setStartDate(today.minusDays(5));
        p2.setEndDate(today.plusMonths(4));
        p2.setDiscountPercent(BigDecimal.valueOf(10));
        p2.setMinimumNights(1);
        p2.setPromoCode("WEEKEND10");
        p2.setImageUrl("/images/nhu-garden-pool-villa.jpg");
        p2.setDisplayOrder(2);
        for (Room r : List.of(r1, r2, r3, r4, r5, r6)) {
            if (r != null) p2.getRooms().add(r);
        }
        promotionRepository.save(p2);

        Promotion p3 = new Promotion();
        p3.setTitle("Honeymoon Special");
        p3.setSubtitle("Gói kỷ niệm & trăng mật");
        p3.setDescription("Dành riêng cho các cặp đôi với ưu đãi 20%, rượu vang chào mừng và bàn tiệc tối lãng mạn bên hồ bơi.");
        p3.setTermsConditions("Áp dụng cho đặt phòng từ 2 đêm trở lên.");
        p3.setCategory(PromotionCategory.HONEYMOON);
        p3.setIsActive(true);
        p3.setIsFeatured(true);
        p3.setStartDate(today.minusDays(5));
        p3.setEndDate(today.plusMonths(6));
        p3.setCountdownEndDate(LocalDateTime.now().plusDays(30));
        p3.setDiscountPercent(BigDecimal.valueOf(20));
        p3.setMinimumNights(2);
        p3.setMinimumBookingAmount(BigDecimal.valueOf(400));
        p3.setPromoCode("HONEYMOON20");
        p3.setImageUrl("/images/nhu-private-dining.jpg");
        p3.setDisplayOrder(3);
        if (r1 != null) p3.getRooms().add(r1);
        if (r5 != null) p3.getRooms().add(r5);
        if (r6 != null) p3.getRooms().add(r6);
        promotionRepository.save(p3);

        Promotion p4 = new Promotion();
        p4.setTitle("Family Fun");
        p4.setSubtitle("Gói nghỉ dưỡng gia đình");
        p4.setDescription("Ưu đãi đặc biệt cho gia đình: giảm 12% kèm tiệc BBQ sân vườn miễn phí.");
        p4.setTermsConditions("Áp dụng cho kỳ nghỉ từ 2 đêm trở lên.");
        p4.setCategory(PromotionCategory.FAMILY);
        p4.setIsActive(true);
        p4.setIsFeatured(false);
        p4.setStartDate(today.minusDays(5));
        p4.setEndDate(today.plusMonths(4));
        p4.setDiscountPercent(BigDecimal.valueOf(12));
        p4.setMinimumNights(2);
        p4.setPromoCode("FAMILY12");
        p4.setImageUrl("/images/nhu-villa-interior.jpg");
        p4.setDisplayOrder(4);
        if (r2 != null) p4.getRooms().add(r2);
        if (r4 != null) p4.getRooms().add(r4);
        promotionRepository.save(p4);

        Promotion p5 = new Promotion();
        p5.setTitle("Early Bird");
        p5.setSubtitle("Đặt sớm - Tiết kiệm lớn");
        p5.setDescription("Nhận ngay 18% ưu đãi khi lên kế hoạch sớm cho kỳ nghỉ tại Nhu Villas.");
        p5.setTermsConditions("Áp dụng cho kỳ nghỉ từ 2 đêm trở lên.");
        p5.setCategory(PromotionCategory.EARLY_BIRD);
        p5.setIsActive(true);
        p5.setIsFeatured(true);
        p5.setStartDate(today.minusDays(5));
        p5.setEndDate(today.plusMonths(6));
        p5.setDiscountPercent(BigDecimal.valueOf(18));
        p5.setMinimumNights(2);
        p5.setPromoCode("EARLYBIRD18");
        p5.setImageUrl("/images/nhu-hero-villa.jpg");
        p5.setDisplayOrder(5);
        if (r1 != null) p5.getRooms().add(r1);
        if (r3 != null) p5.getRooms().add(r3);
        promotionRepository.save(p5);

        Promotion p6 = new Promotion();
        p6.setTitle("Long Stay Discount");
        p6.setSubtitle("Kỳ nghỉ dài ngày");
        p6.setDescription("Giảm 25% cho kỳ nghỉ từ 5 đêm trở lên kèm dịch vụ giặt ủi và đưa đón sân bay miễn phí.");
        p6.setTermsConditions("Áp dụng cho đặt phòng từ 5 đêm trở lên.");
        p6.setCategory(PromotionCategory.LONG_STAY);
        p6.setIsActive(true);
        p6.setIsFeatured(false);
        p6.setStartDate(today.minusDays(5));
        p6.setEndDate(today.plusMonths(12));
        p6.setDiscountPercent(BigDecimal.valueOf(25));
        p6.setMinimumNights(5);
        p6.setPromoCode("LONGSTAY25");
        p6.setImageUrl("/images/nhu-location-landscape.jpg");
        p6.setDisplayOrder(6);
        if (r4 != null) p6.getRooms().add(r4);
        promotionRepository.save(p6);
    }

    private void initializeVouchers() {
        createVoucherIfMissing("WELCOME10", BigDecimal.valueOf(10), true, 100, LocalDate.now().plusYears(1));
        createVoucherIfMissing("NHUVILLAS20", BigDecimal.valueOf(20), true, 50, LocalDate.now().plusYears(1));
        createVoucherIfMissing("VIP100", BigDecimal.valueOf(100), false, 50, LocalDate.now().plusYears(1));
    }

    private void createVoucherIfMissing(String code, BigDecimal amount, boolean isPercent, int quantity, LocalDate expiry) {
        if (voucherRepository.findByCodeIgnoreCase(code).isEmpty()) {
            Voucher v = new Voucher();
            v.setCode(code);
            v.setAmount(amount);
            v.setPercent(isPercent);
            v.setQuantity(quantity);
            v.setExpiryDate(expiry);
            voucherRepository.save(v);
        }
    }

    private void initializeSampleReviews() {
        if (reviewRepository.count() > 0) return;

        User reviewer1 = getOrCreateReviewer("minh.anh.guest", "Nguyễn Minh Anh", "minhanh.guest@nhuvillas.com");
        User reviewer2 = getOrCreateReviewer("alex.chen.guest", "Alexander Chen", "alexchen.guest@nhuvillas.com");
        User reviewer3 = getOrCreateReviewer("thao.le.guest", "Lê Phương Thảo", "thaole.guest@nhuvillas.com");

        for (Room room : roomRepository.findAll()) {
            createSampleReview(
                reviewer1, room, 5, 5, 5, 5, 5, 5,
                "Không gian biệt thự thực sự yên bình, hồ bơi nước ấm nhìn thẳng ra rừng thông Đà Lạt. Bữa sáng phục vụ tại phòng rất tinh tế và chu đáo.",
                "Cảm ơn chị Minh Anh đã lựa chọn Nhu Villas. Hẹn sớm được đón tiếp chị quay lại Đà Lạt!"
            );
            createSampleReview(
                reviewer2, room, 5, 5, 5, 5, 5, 5,
                "Architectural perfection. Total privacy, warm hospitality from our butler, and waking up to the mist over Tuyen Lam Lake was unforgettable.",
                "Thank you Alexander for your kind words! We look forward to welcoming you back to Nhu Villas."
            );
            createSampleReview(
                reviewer3, room, 5, 5, 5, 5, 4, 5,
                "Gia đình mình đã có kỳ nghỉ 3 đêm tuyệt vời. Nội thất gỗ ấm cúng, mùi tinh dầu thông dễ chịu và dịch vụ quản gia hỗ trợ 24/7 rất chuyên nghiệp.",
                null
            );
        }
    }

    private User getOrCreateReviewer(String username, String fullName, String email) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User u = new User();
            u.setUsername(username);
            u.setFullName(fullName);
            u.setEmail(email);
            u.setPassword(passwordEncoder.encode("GuestReview@2026!"));
            u.setRole(UserRole.USER);
            u.setEmailVerified(true);
            return userRepository.save(u);
        });
    }

    private void createSampleReview(
            User user, Room room,
            int rating, int clean, int service, int loc, int val, int amen,
            String comment, String adminReply
    ) {
        Review r = new Review();
        r.setUser(user);
        r.setRoom(room);
        r.setRating(rating);
        r.setRatingCleanliness(clean);
        r.setRatingService(service);
        r.setRatingLocation(loc);
        r.setRatingValue(val);
        r.setRatingAmenities(amen);
        r.setComment(comment);
        r.setCreatedAt(LocalDateTime.now().minusDays(7));
        if (adminReply != null) {
            r.setAdminReply(adminReply);
            r.setAdminReplyAt(LocalDateTime.now().minusDays(6));
        }
        reviewRepository.save(r);
    }
}
