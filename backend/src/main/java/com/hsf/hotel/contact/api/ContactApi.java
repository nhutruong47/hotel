package com.hsf.hotel.contact.api;
import com.hsf.hotel.user.model.User;

import com.hsf.hotel.contact.model.ContactMessage;
import com.hsf.hotel.contact.model.ContactMessage.ContactType;
import com.hsf.hotel.contact.service.ContactService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
public class ContactApi {

    private final ContactService contactService;

    public static class ContactRequest {
        @NotBlank
        @Size(max = 255)
        public String name;

        @NotBlank
        @Email
        @Size(max = 255)
        public String email;

        @Size(max = 64)
        public String phone;

        @NotBlank
        @Size(max = 255)
        public String subject;

        @NotBlank
        @Size(max = 5000)
        public String message;

        @Size(max = 32)
        public String type;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> submitContact(
            @Valid @RequestBody ContactRequest body,
            HttpServletRequest request) {
        String typeStr = body.type;

        ContactType type = ContactType.GENERAL;
        if (typeStr != null) {
            try {
                type = ContactType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        ContactMessage contact = contactService.submitContact(
                body.name.trim(), body.email.trim(), body.phone, body.subject.trim(), body.message.trim(), type,
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Tin nhắn của bạn đã được gửi thành công. Chúng tôi sẽ liên hệ lại sớm nhất có thể.",
                "ticketId", "TKT-" + String.format("%06d", contact.getId())
        ));
    }
}
