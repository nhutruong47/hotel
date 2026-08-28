package com.hsf.hotel.repository;

import com.hsf.hotel.model.ContactMessage;
import com.hsf.hotel.model.ContactMessage.ContactStatus;
import com.hsf.hotel.model.ContactMessage.ContactType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Integer> {

    List<ContactMessage> findByStatusOrderByCreatedAtDesc(ContactStatus status);

    List<ContactMessage> findByTypeOrderByCreatedAtDesc(ContactType type);

    Page<ContactMessage> findByStatusOrderByCreatedAtDesc(ContactStatus status, Pageable pageable);

    long countByStatus(ContactStatus status);

    long countByStatusAndType(ContactStatus status, ContactType type);
}
