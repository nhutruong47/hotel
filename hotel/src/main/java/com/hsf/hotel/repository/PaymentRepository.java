package com.hsf.hotel.repository;

import com.hsf.hotel.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    List<Payment> findByBookingIdOrderByCreatedAtDesc(Integer bookingId);

    Optional<Payment> findByTransactionRef(String transactionRef);

    Optional<Payment> findByIntentId(String intentId);

    @Query("SELECT p FROM Payment p WHERE p.booking.user.id = :userId ORDER BY p.createdAt DESC")
    List<Payment> findByUserId(@Param("userId") Integer userId);

    @Query("SELECT p FROM Payment p WHERE p.status = :status ORDER BY p.createdAt DESC")
    List<Payment> findByStatusOrderByCreatedAtDesc(@Param("status") Payment.PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'PAID' " +
            "AND p.createdAt >= :start AND p.createdAt < :end")
    java.math.BigDecimal sumCompletedAmountInRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}