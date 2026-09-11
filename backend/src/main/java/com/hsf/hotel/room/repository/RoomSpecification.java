package com.hsf.hotel.room.repository;

import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.booking.model.BookingStatus;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.RoomTypeEntity;
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

            // Subquery 1: Check conflicting bookings
            Subquery<Integer> bookingSubquery = query.subquery(Integer.class);
            Root<Booking> bookingRoot = bookingSubquery.from(Booking.class);
            bookingSubquery.select(cb.literal(1));
            bookingSubquery.where(
                cb.equal(bookingRoot.get("room").get("id"), root.get("id")),
                bookingRoot.get("status").in(List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.PAID,
                        BookingStatus.CHECKED_IN, BookingStatus.CHECKED_OUT, BookingStatus.COMPLETED)),
                cb.lessThan(bookingRoot.get("checkInDate"), checkOut),
                cb.greaterThan(bookingRoot.get("checkOutDate"), checkIn)
            );

            // Subquery 2: Check conflicting maintenances
            Subquery<Integer> maintenanceSubquery = query.subquery(Integer.class);
            Root<com.hsf.hotel.room.model.VillaMaintenance> maintRoot = maintenanceSubquery.from(com.hsf.hotel.room.model.VillaMaintenance.class);
            maintenanceSubquery.select(cb.literal(1));
            maintenanceSubquery.where(
                cb.equal(maintRoot.get("room").get("id"), root.get("id")),
                maintRoot.get("status").in(List.of(
                    com.hsf.hotel.room.model.VillaMaintenance.MaintenanceStatus.SCHEDULED,
                    com.hsf.hotel.room.model.VillaMaintenance.MaintenanceStatus.IN_PROGRESS
                )),
                cb.lessThan(maintRoot.get("startDate"), checkOut),
                cb.greaterThan(maintRoot.get("endDate"), checkIn)
            );

            return cb.and(cb.not(cb.exists(bookingSubquery)), cb.not(cb.exists(maintenanceSubquery)));
        };
    }

    public static Specification<Room> hasPromotion(String promotion) {
        return (root, query, cb) -> {
            if (promotion == null || promotion.trim().isEmpty()) return null;
            query.distinct(true);
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
