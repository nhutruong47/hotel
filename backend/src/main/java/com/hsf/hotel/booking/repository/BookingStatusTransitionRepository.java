package com.hsf.hotel.booking.repository;

import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.booking.model.BookingStatusTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingStatusTransitionRepository extends JpaRepository<BookingStatusTransition, Integer> {

    List<BookingStatusTransition> findByBookingOrderByCreatedAtAsc(Booking booking);
}
