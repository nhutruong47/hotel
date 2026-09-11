package com.hsf.hotel.user.repository;

import com.hsf.hotel.user.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Integer id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByVerificationToken(String token);

    Optional<User> findByResetToken(String resetToken);
}
