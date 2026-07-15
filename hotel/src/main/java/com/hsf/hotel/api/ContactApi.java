package com.hsf.hotel.api;

import com.hsf.hotel.model.ContactMessage;
import com.hsf.hotel.model.ContactMessage.ContactPriority;
import com.hsf.hotel.model.ContactMessage.ContactStatus;
import com.hsf.hotel.model.ContactMessage.ContactType;
import com.hsf.hotel.service.ContactService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
public class ContactApi {

    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> submitContact(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String name = body.get("name");
        String email = body.get("email");
        String phone = body.get("phone");
        String subject = body.get("subject");
        String message = body.get("message");
        String typeStr = body.get("type");

        ContactType type = ContactType.GENERAL;
        if (typeStr != null) {
            try {
                type = ContactType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        ContactMessage contact = contactService.submitContact(
                name, email, phone, subject, message, type,
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Tin nhắn của bạn đã được gửi thành công. Chúng tôi sẽ liên hệ lại sớm nhất có thể.",
                "ticketId", "TKT-" + String.format("%06d", contact.getId())
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(contactService.getContactStats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactMessage> getContact(@PathVariable Integer id) {
        return ResponseEntity.ok(contactService.getContactById(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ContactMessage> updateStatus(
            @PathVariable Integer id,
            @RequestParam ContactStatus status,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Integer assignedTo) {
        return ResponseEntity.ok(contactService.updateStatus(id, status, notes, assignedTo));
    }

    @PutMapping("/{id}/priority")
    public ResponseEntity<ContactMessage> updatePriority(
            @PathVariable Integer id,
            @RequestParam ContactPriority priority) {
        return ResponseEntity.ok(contactService.updatePriority(id, priority));
    }
}
