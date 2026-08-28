package com.hsf.hotel.config;

import com.hsf.hotel.model.FaqItem;
import com.hsf.hotel.model.FaqItem.FaqCategory;
import com.hsf.hotel.model.Promotion;
import com.hsf.hotel.model.Promotion.PromotionCategory;
import com.hsf.hotel.repository.FaqItemRepository;
import com.hsf.hotel.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Seeds demo data for fresh environments (default + dev profile). The
 * {@code prod} profile deliberately excludes this runner so a production
 * boot cannot accidentally insert sample rooms, FAQs, and promotions over
 * the real catalogue. Real production data must be loaded via Flyway
 * migrations and / or admin tools.
 */
@Component
@Profile({"default", "dev"})
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final FaqItemRepository faqRepository;
    private final PromotionRepository promotionRepository;
    private final com.hsf.hotel.repository.RoomRepository roomRepository;
    private final com.hsf.hotel.repository.RoomTypeRepository roomTypeRepository;
    private final com.hsf.hotel.repository.AmenityRepository amenityRepository;

    @Override
    public void run(String... args) {
        initializeFaqs();
        initializeRooms();
        initializePromotions();
    }

    private void initializeRooms() {
        if (roomRepository.count() > 0) return;

        // Create Room Types
        com.hsf.hotel.model.RoomTypeEntity poolVilla = new com.hsf.hotel.model.RoomTypeEntity();
        poolVilla.setName("Pool Villa");
        poolVilla.setDescription("A one-bedroom garden villa with a private pool.");
        poolVilla = roomTypeRepository.save(poolVilla);

        com.hsf.hotel.model.RoomTypeEntity familyVilla = new com.hsf.hotel.model.RoomTypeEntity();
        familyVilla.setName("Family Villa");
        familyVilla.setDescription("Two quiet bedrooms, generous living space.");
        familyVilla = roomTypeRepository.save(familyVilla);

        com.hsf.hotel.model.RoomTypeEntity residence = new com.hsf.hotel.model.RoomTypeEntity();
        residence.setName("Residence");
        residence.setDescription("A larger private residence for hosted dinners.");
        residence = roomTypeRepository.save(residence);

        com.hsf.hotel.model.RoomTypeEntity signature = new com.hsf.hotel.model.RoomTypeEntity();
        signature.setName("Signature");
        signature.setDescription("The largest estate in our collection.");
        signature = roomTypeRepository.save(signature);

        // Create Amenities
        com.hsf.hotel.model.Amenity pool = new com.hsf.hotel.model.Amenity();
        pool.setName("Private pool");
        pool = amenityRepository.save(pool);

        com.hsf.hotel.model.Amenity breakfast = new com.hsf.hotel.model.Amenity();
        breakfast.setName("Breakfast service");
        breakfast = amenityRepository.save(breakfast);

        com.hsf.hotel.model.Amenity wifi = new com.hsf.hotel.model.Amenity();
        wifi.setName("High-speed Wi-Fi");
        wifi = amenityRepository.save(wifi);

        // Seed Room 1
        com.hsf.hotel.model.Room r1 = new com.hsf.hotel.model.Room();
        r1.setRoomNumber("GV-01");
        r1.setRoomType(poolVilla);
        r1.setDescription("A one-bedroom garden villa with a private pool, shaded terrace, and open-air living room.");
        r1.setImageUrl("/images/nhu-garden-pool-villa.jpg");
        r1.setPricePerNight(BigDecimal.valueOf(450));
        r1.setCapacity(2);
        r1.setBedrooms(1);
        r1.setIsAvailable(true);
        r1.getAmenities().add(pool);
        r1.getAmenities().add(breakfast);
        r1.setAvgRating(BigDecimal.valueOf(4.9));
        r1.setReviewCount(38L);
        roomRepository.save(r1);

        // Seed Room 2
        com.hsf.hotel.model.Room r2 = new com.hsf.hotel.model.Room();
        r2.setRoomNumber("TF-02");
        r2.setRoomType(familyVilla);
        r2.setDescription("Two quiet bedrooms, generous living space, and a terrace prepared for slow family days.");
        r2.setImageUrl("/images/nhu-villa-interior.jpg");
        r2.setPricePerNight(BigDecimal.valueOf(720));
        r2.setCapacity(4);
        r2.setBedrooms(2);
        r2.setIsAvailable(true);
        r2.getAmenities().add(wifi);
        r2.setAvgRating(BigDecimal.valueOf(4.8));
        r2.setReviewCount(27L);
        roomRepository.save(r2);

        // Seed Room 3
        com.hsf.hotel.model.Room r3 = new com.hsf.hotel.model.Room();
        r3.setRoomNumber("HR-03");
        r3.setRoomType(residence);
        r3.setDescription("A larger private residence for hosted dinners, longer retreats, and sunset pool rituals.");
        r3.setImageUrl("/images/nhu-infinity-pool.jpg");
        r3.setPricePerNight(BigDecimal.valueOf(1150));
        r3.setCapacity(6);
        r3.setBedrooms(3);
        r3.setIsAvailable(true);
        r3.getAmenities().add(pool);
        r3.setAvgRating(BigDecimal.valueOf(4.95));
        r3.setReviewCount(19L);
        roomRepository.save(r3);

        // Seed Room 4
        com.hsf.hotel.model.Room r4 = new com.hsf.hotel.model.Room();
        r4.setRoomNumber("SE-04");
        r4.setRoomType(signature);
        r4.setDescription("The largest estate in our collection, offering panoramic views, four bedrooms, and complete privacy.");
        r4.setImageUrl("/images/nhu-private-dining.jpg");
        r4.setPricePerNight(BigDecimal.valueOf(2500));
        r4.setCapacity(8);
        r4.setBedrooms(4);
        r4.setIsAvailable(true);
        r4.getAmenities().add(pool);
        r4.getAmenities().add(breakfast);
        r4.getAmenities().add(wifi);
        r4.setAvgRating(BigDecimal.valueOf(5.0));
        r4.setReviewCount(12L);
        roomRepository.save(r4);
    }

    private void initializeFaqs() {
        if (faqRepository.count() > 0) return;

        faqRepository.save(new FaqItem(
            "Làm thế nào để đặt phòng?",
            "Bạn có thể đặt phòng trực tiếp trên website của chúng tôi bằng cách chọn villa, ngày check-in/check-out, và hoàn tất thanh toán. Nếu bạn cần hỗ trợ, hãy liên hệ với đội ngũ reservations qua email hoặc điện thoại.",
            FaqCategory.BOOKING
        ));
        faqRepository.save(new FaqItem(
            "Tôi có thể hủy phòng không?",
            "Bạn có thể hủy phòng theo chính sách linh hoạt: hủy trước 7 ngày sẽ được hoàn tiền 100%, hủy trước 3-7 ngày sẽ được hoàn 50%, hủy trong vòng 3 ngày sẽ không được hoàn tiền. Thời gian áp dụng có thể thay đổi tùy theo mùa cao điểm.",
            FaqCategory.CANCELLATION
        ));
        faqRepository.save(new FaqItem(
            "Phương thức thanh toán nào được chấp nhận?",
            "Chúng tôi chấp nhận thanh toán qua thẻ tín dụng (Visa, Mastercard), chuyển khoản ngân hàng, và thanh toán tiền mặt trực tiếp tại quầy.",
            FaqCategory.PAYMENT
        ));
        faqRepository.save(new FaqItem(
            "Quy định về hoàn tiền như thế nào?",
            "Hoàn tiền được xử lý trong vòng 7-14 ngày làm việc sau khi yêu cầu được duyệt. Thời gian hoàn tiền có thể thay đổi tùy theo ngân hàng của bạn.",
            FaqCategory.REFUND
        ));
        faqRepository.save(new FaqItem(
            "Giờ check-in và check-out là mấy giờ?",
            "Giờ check-in là 14:00 và giờ check-out là 11:00. Bạn có thể yêu cầu early check-in hoặc late check-out tùy theo tình trạng phòng, phí có thể áp dụng.",
            FaqCategory.POLICIES
        ));
        faqRepository.save(new FaqItem(
            "Villa có bao gồm bữa sáng không?",
            "Tùy theo gói booking, một số villa bao gồm bữa sáng. Vui lòng kiểm tra chi tiết gói trước khi đặt hoặc liên hệ với đội ngũ để biết thêm thông tin.",
            FaqCategory.SERVICES
        ));
        faqRepository.save(new FaqItem(
            "Villa có những tiện ích gì?",
            "Mỗi villa đều được trang bị: hồ bơi riêng, điều hòa không khí, Wi-Fi tốc độ cao, bếp đầy đủ, máy giặt, xe đạp miễn phí, và dịch vụ dọn phòng hàng ngày.",
            FaqCategory.FACILITIES
        ));
        faqRepository.save(new FaqItem(
            "Cách di chuyển từ sân bay đến villa?",
            "Chúng tôi cung cấp dịch vụ đưa đón sân bay với xe riêng. Thời gian di chuyển khoảng 35 phút. Bạn có thể đặt dịch vụ này khi đặt phòng hoặc liên hệ trực tiếp với chúng tôi.",
            FaqCategory.TRANSPORT
        ));
        faqRepository.save(new FaqItem(
            "Tôi có thể ở lâu hơn nếu cần không?",
            "Nếu bạn muốn gia hạn, vui lòng thông báo cho chúng tôi ít nhất 2 ngày trước ngày check-out dự kiến. Chúng tôi sẽ kiểm tra tình trạng phòng và thông báo cho bạn sớm nhất có thể.",
            FaqCategory.BOOKING
        ));
        faqRepository.save(new FaqItem(
            "Có chỗ đỗ xe không?",
            "Có, chúng tôi có bãi đỗ xe riêng miễn phí cho tất cả khách lưu trú. Bãi đỗ xe có camera an ninh 24/7.",
            FaqCategory.FACILITIES
        ));
    }

    private void initializePromotions() {
        if (promotionRepository.count() > 0) return;

        java.util.List<com.hsf.hotel.model.Room> rooms = roomRepository.findAll();
        com.hsf.hotel.model.Room r1 = rooms.stream().filter(r -> "GV-01".equals(r.getRoomNumber())).findFirst().orElse(null);
        com.hsf.hotel.model.Room r2 = rooms.stream().filter(r -> "TF-02".equals(r.getRoomNumber())).findFirst().orElse(null);
        com.hsf.hotel.model.Room r3 = rooms.stream().filter(r -> "HR-03".equals(r.getRoomNumber())).findFirst().orElse(null);
        com.hsf.hotel.model.Room r4 = rooms.stream().filter(r -> "SE-04".equals(r.getRoomNumber())).findFirst().orElse(null);

        LocalDate today = LocalDate.now();

        Promotion p1 = new Promotion();
        p1.setTitle("Summer Escape");
        p1.setSubtitle("Giảm 15% cho kỳ nghỉ mùa hè");
        p1.setDescription("Tận hưởng mùa hè tuyệt vời với ưu đãi giảm 15% cho tất cả các villa. Áp dụng cho đặt phòng từ 3 đêm trở lên.");
        p1.setTermsConditions("Chỉ áp dụng cho đặt phòng từ 3 đêm. Không áp dụng kết hợp với các khuyến mãi khác. Yêu cầu thanh toán trước 100%.");
        p1.setCategory(PromotionCategory.SEASONAL);
        if (r3 != null) p1.getRooms().add(r3);
        p1.setIsActive(true);
        p1.setIsFeatured(true);
        p1.setStartDate(today);
        p1.setEndDate(today.plusMonths(2));
        p1.setCountdownEndDate(LocalDateTime.now().plusDays(14));
        p1.setDiscountPercent(BigDecimal.valueOf(15));
        p1.setMinimumNights(3);
        p1.setMinimumBookingAmount(BigDecimal.valueOf(300));
        p1.setImageUrl("/images/nhu-infinity-pool.jpg");
        p1.setDisplayOrder(1);
        promotionRepository.save(p1);

        Promotion p2 = new Promotion();
        p2.setTitle("Weekend Retreat");
        p2.setSubtitle("Cuối tuần đặc biệt");
        p2.setDescription("Ưu đãi 10% cho các booking vào cuối tuần (thứ 6, thứ 7, chủ nhật). Lý tưởng cho những chuyến đi ngắn ngày.");
        p2.setTermsConditions("Áp dụng cho check-in vào thứ 6, thứ 7 hoặc chủ nhật. Không giới hạn số đêm.");
        p2.setCategory(PromotionCategory.WEEKEND);
        p2.setIsActive(true);
        p2.setIsFeatured(false);
        p2.setStartDate(today);
        p2.setEndDate(today.plusMonths(3));
        p2.setDiscountPercent(BigDecimal.valueOf(10));
        p2.setMinimumNights(1);
        p2.setPromoCode("WEEKEND10");
        p2.setDisplayOrder(2);
        if (r1 != null) p2.getRooms().add(r1);
        if (r2 != null) p2.getRooms().add(r2);
        if (r3 != null) p2.getRooms().add(r3);
        if (r4 != null) p2.getRooms().add(r4);
        promotionRepository.save(p2);

        Promotion p3 = new Promotion();
        p3.setTitle("Honeymoon Special");
        p3.setSubtitle("Gói tuần trăng mật");
        p3.setDescription("Dành riêng cho các cặp đôi mới cưới với ưu đãi 20% và champagne chào mừng miễn phí. Bao gồm private dinner 1 bữa.");
        p3.setTermsConditions("Yêu cầu xác nhận đám cưới trong vòng 6 tháng. Private dinner cần đặt trước 48 giờ.");
        p3.setCategory(PromotionCategory.HONEYMOON);
        p3.setIsActive(true);
        p3.setIsFeatured(true);
        p3.setStartDate(today);
        p3.setEndDate(today.plusMonths(6));
        p3.setCountdownEndDate(LocalDateTime.now().plusDays(30));
        p3.setDiscountPercent(BigDecimal.valueOf(20));
        p3.setMinimumNights(2);
        p3.setMinimumBookingAmount(BigDecimal.valueOf(500));
        p3.setImageUrl("/images/nhu-private-dining.jpg");
        p3.setDisplayOrder(3);
        if (r1 != null) p3.getRooms().add(r1);
        promotionRepository.save(p3);

        Promotion p4 = new Promotion();
        p4.setTitle("Family Fun");
        p4.setSubtitle("Gói gia đình");
        p4.setDescription("Ưu đãi đặc biệt cho gia đình 4 người trở lên: giảm 12% và miễn phí 1 bữa ăn trẻ em mỗi ngày.");
        p4.setTermsConditions("Áp dụng cho booking có ít nhất 2 trẻ em dưới 12 tuổi. Tối thiểu 3 đêm.");
        p4.setCategory(PromotionCategory.FAMILY);
        p4.setIsActive(true);
        p4.setIsFeatured(false);
        p4.setStartDate(today);
        p4.setEndDate(today.plusMonths(4));
        p4.setDiscountPercent(BigDecimal.valueOf(12));
        p4.setMinimumNights(3);
        p4.setPromoCode("FAMILY12");
        p4.setDisplayOrder(4);
        if (r2 != null) p4.getRooms().add(r2);
        promotionRepository.save(p4);

        Promotion p5 = new Promotion();
        p5.setTitle("Early Bird");
        p5.setSubtitle("Đặt sớm - Tiết kiệm lớn");
        p5.setDescription("Đặt phòng trước 30 ngày và nhận ngay 18% giảm giá. Cơ hội tốt nhất để có được villa ưng ý với giá tốt nhất.");
        p5.setTermsConditions("Yêu cầu đặt trước ít nhất 30 ngày so với ngày check-in. Không hoàn tiền nếu hủy.");
        p5.setCategory(PromotionCategory.EARLY_BIRD);
        p5.setIsActive(true);
        p5.setIsFeatured(true);
        p5.setStartDate(today);
        p5.setEndDate(today.plusMonths(6));
        p5.setDiscountPercent(BigDecimal.valueOf(18));
        p5.setMinimumNights(2);
        p5.setPromoCode("EARLYBIRD18");
        p5.setDisplayOrder(5);
        if (r1 != null) p5.getRooms().add(r1);
        if (r3 != null) p5.getRooms().add(r3);
        promotionRepository.save(p5);

        Promotion p6 = new Promotion();
        p6.setTitle("Long Stay Discount");
        p6.setSubtitle("Ở lâu - Trả ít hơn");
        p6.setDescription("Giảm 25% cho booking từ 7 đêm trở lên. Càng ở lâu, càng tiết kiệm nhiều!");
        p6.setTermsConditions("Áp dụng cho đặt phòng từ 7 đêm trở lên. Miễn phí laundry service.");
        p6.setCategory(PromotionCategory.LONG_STAY);
        p6.setIsActive(true);
        p6.setIsFeatured(false);
        p6.setStartDate(today);
        p6.setEndDate(today.plusMonths(12));
        p6.setDiscountPercent(BigDecimal.valueOf(25));
        p6.setMinimumNights(7);
        p6.setPromoCode("LONGSTAY25");
        p6.setDisplayOrder(6);
        if (r4 != null) p6.getRooms().add(r4);
        promotionRepository.save(p6);
    }
}
