package com.hsf.hotel.payment.repository;

import com.hsf.hotel.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    List<Payment> findByBookingIdOrderByCreatedAtDesc(Integer bookingId);

    Optional<Payment> findByTransactionRef(String transactionRef);

    Optional<Payment> findByIntentId(String intentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.intentId = :intentId")
    Optional<Payment> findByIntentIdForUpdate(@Param("intentId") String intentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.transactionRef = :transactionRef")
    Optional<Payment> findByTransactionRefForUpdate(@Param("transactionRef") String transactionRef);

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
