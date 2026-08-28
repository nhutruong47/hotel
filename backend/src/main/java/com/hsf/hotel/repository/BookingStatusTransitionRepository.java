package com.hsf.hotel.repository;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.BookingStatusTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingStatusTransitionRepository extends JpaRepository<BookingStatusTransition, Integer> {

    List<BookingStatusTransition> findByBookingOrderByCreatedAtAsc(Booking booking);
}
