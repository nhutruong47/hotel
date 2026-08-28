package com.hsf.hotel.service;

import com.hsf.hotel.model.ContactMessage;
import com.hsf.hotel.model.ContactMessage.ContactPriority;
import com.hsf.hotel.model.ContactMessage.ContactStatus;
import com.hsf.hotel.model.ContactMessage.ContactType;
import com.hsf.hotel.model.User;
import com.hsf.hotel.repository.ContactMessageRepository;
import com.hsf.hotel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactMessageRepository contactRepository;
    private final UserRepository userRepository;

    @Transactional
    public ContactMessage submitContact(String name, String email, String phone, String subject, String message,
                                       ContactType type, String ipAddress, String userAgent) {
        ContactMessage contact = new ContactMessage();
        contact.setName(name);
        contact.setEmail(email);
        contact.setPhone(phone);
        contact.setSubject(subject);
        contact.setMessage(message);
        contact.setType(type != null ? type : ContactType.GENERAL);
        contact.setIpAddress(ipAddress);
        contact.setUserAgent(userAgent);
        contact.setCreatedAt(LocalDateTime.now());
        return contactRepository.save(contact);
    }

    public List<ContactMessage> getAllContacts() {
        return contactRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Page<ContactMessage> getContactsByStatus(ContactStatus status, int page, int size) {
        return contactRepository.findByStatusOrderByCreatedAtDesc(status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public List<ContactMessage> getContactsByStatus(ContactStatus status) {
        return contactRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Map<String, Long> getContactStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", contactRepository.count());
        stats.put("pending", contactRepository.countByStatus(ContactStatus.PENDING));
        stats.put("inProgress", contactRepository.countByStatus(ContactStatus.IN_PROGRESS));
        stats.put("resolved", contactRepository.countByStatus(ContactStatus.RESOLVED));
        stats.put("closed", contactRepository.countByStatus(ContactStatus.CLOSED));
        return stats;
    }

    @Transactional
    public ContactMessage updateStatus(Integer id, ContactStatus status, String adminNotes, Integer assignedToId) {
        ContactMessage contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact message not found"));

        contact.setStatus(status);
        if (adminNotes != null) {
            contact.setAdminNotes(adminNotes);
        }
        if (assignedToId != null) {
            User assignedUser = userRepository.findById(assignedToId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            contact.setAssignedTo(assignedUser);
        }
        if (status == ContactStatus.RESOLVED && contact.getRespondedAt() == null) {
            contact.setRespondedAt(LocalDateTime.now());
        }
        contact.setUpdatedAt(LocalDateTime.now());

        return contactRepository.save(contact);
    }

    @Transactional
    public ContactMessage updatePriority(Integer id, ContactPriority priority) {
        ContactMessage contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact message not found"));
        contact.setPriority(priority);
        contact.setUpdatedAt(LocalDateTime.now());
        return contactRepository.save(contact);
    }

    public ContactMessage getContactById(Integer id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact message not found"));
    }
}
