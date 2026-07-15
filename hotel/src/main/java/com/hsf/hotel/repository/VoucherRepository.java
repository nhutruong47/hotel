package com.hsf.hotel.repository;

import com.hsf.hotel.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    Optional<Voucher> findByCodeIgnoreCase(String code);

    /**
     * Atomically decrement {@code quantity} if and only if at least one unit
     * remains. Returns the number of rows actually updated so callers can
     * detect a race where two concurrent bookings both pass the
     * pre-transaction check but only one should win.
     */
    @Modifying
    @Query("UPDATE Voucher v SET v.quantity = v.quantity - 1 " +
            "WHERE v.id = :id AND v.quantity > 0")
    int decrementQuantityAtomic(@Param("id") Integer id);

    /**
     * Release a previously-consumed voucher unit. Used when a booking is
     * cancelled or expired before the voucher is actually applied to a charge.
     */
    @Modifying
    @Query("UPDATE Voucher v SET v.quantity = v.quantity + 1 WHERE v.id = :id")
    int incrementQuantityAtomic(@Param("id") Integer id);
}

