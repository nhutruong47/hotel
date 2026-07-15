package com.hsf.hotel.repository;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.RoomTypeEntity;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class RoomSpecification {

    public static Specification<Room> isAvailable() {
        return (root, query, cb) -> cb.isTrue(root.get("isAvailable"));
    }

    public static Specification<Room> hasCapacityGreaterThanEqual(Integer capacity) {
        return (root, query, cb) -> {
            if (capacity == null) return null;
            return cb.greaterThanOrEqualTo(root.get("capacity"), capacity);
        };
    }

    public static Specification<Room> hasBedroomsGreaterThanEqual(Integer bedrooms) {
        return (root, query, cb) -> {
            if (bedrooms == null) return null;
            return cb.greaterThanOrEqualTo(root.get("bedrooms"), bedrooms);
        };
    }

    public static Specification<Room> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) return null;
            if (minPrice != null && maxPrice != null) {
                return cb.between(root.get("pricePerNight"), minPrice, maxPrice);
            } else if (minPrice != null) {
                return cb.greaterThanOrEqualTo(root.get("pricePerNight"), minPrice);
            } else {
                return cb.lessThanOrEqualTo(root.get("pricePerNight"), maxPrice);
            }
        };
    }

    public static Specification<Room> hasRoomType(Integer roomTypeId) {
        return (root, query, cb) -> {
            if (roomTypeId == null) return null;
            Join<Room, RoomTypeEntity> roomTypeJoin = root.join("roomType", JoinType.INNER);
            return cb.equal(roomTypeJoin.get("id"), roomTypeId);
        };
    }

    public static Specification<Room> hasAmenities(List<String> amenities) {
        return (root, query, cb) -> {
            if (amenities == null || amenities.isEmpty()) return null;
            
            // To require ALL amenities, we can use a subquery or repeated joins.
            // A simpler approach in Criteria for "has all these amenities":
            Predicate[] predicates = new Predicate[amenities.size()];
            for (int i = 0; i < amenities.size(); i++) {
                Join<Object, Object> amenityJoin = root.join("amenities");
                predicates[i] = cb.equal(amenityJoin.get("name"), amenities.get(i));
            }
            // For requiring all, multiple joins can lead to cross joins.
            // Alternative: Count matching amenities.
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Room> subRoot = subquery.from(Room.class);
            Join<Object, Object> subAmenityJoin = subRoot.join("amenities");
            subquery.select(cb.count(subAmenityJoin.get("id")));
            subquery.where(
                    cb.equal(subRoot.get("id"), root.get("id")),
                    subAmenityJoin.get("name").in(amenities)
            );
            return cb.equal(subquery, (long) amenities.size());
        };
    }

    public static Specification<Room> isNotBooked(LocalDate checkIn, LocalDate checkOut) {
        return (root, query, cb) -> {
            if (checkIn == null || checkOut == null) return null;

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Booking> bookingRoot = subquery.from(Booking.class);
            subquery.select(cb.count(bookingRoot.get("id")));

            subquery.where(
                cb.equal(bookingRoot.get("room").get("id"), root.get("id")),
                bookingRoot.get("status").in(List.of("PENDING_PAYMENT", "PAID",
                        "CHECKED_IN", "CHECKED_OUT", "COMPLETED")),
                cb.lessThan(bookingRoot.get("checkInDate"), checkOut),
                cb.greaterThan(bookingRoot.get("checkOutDate"), checkIn)
            );

            return cb.equal(subquery, 0L);
        };
    }

    public static Specification<Room> hasPromotion(String promotion) {
        return (root, query, cb) -> {
            if (promotion == null || promotion.trim().isEmpty()) return null;
            Join<Object, Object> promotionJoin = root.join("promotions");
            String cleanPromo = promotion.trim().toLowerCase();
            String spacePromo = cleanPromo.replace("-", " ");
            return cb.or(
                cb.equal(cb.lower(promotionJoin.get("promoCode")), cleanPromo),
                cb.equal(cb.lower(promotionJoin.get("title")), cleanPromo),
                cb.equal(cb.lower(promotionJoin.get("title")), spacePromo)
            );
        };
    }
}
