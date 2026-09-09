package com.hsf.hotel.contact.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.config.ApiResponses;
import com.hsf.hotel.contact.dto.ContactRequest;
import com.hsf.hotel.contact.dto.ContactSubmissionResponse;
import com.hsf.hotel.contact.model.ContactMessage;
import com.hsf.hotel.contact.model.ContactMessage.ContactType;
import com.hsf.hotel.contact.service.ContactService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@com.hsf.hotel.config.ApiController
@RequestMapping(com.hsf.hotel.config.ApiPaths.V1 + "/contact")
@RequiredArgsConstructor
public class ContactApi {

    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContactSubmissionResponse>> submitContact(
            @Valid @RequestBody ContactRequest body,
            HttpServletRequest request) {
        ContactType type = ContactType.GENERAL;
        if (body.type() != null) {
            try {
                type = ContactType.valueOf(body.type().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Unknown values deliberately fall back to GENERAL.
            }
        }

        ContactMessage contact = contactService.submitContact(
                body.name().trim(),
                body.email().trim(),
                body.phone(),
                body.subject().trim(),
                body.message().trim(),
                type,
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );

        return ApiResponses.ok(new ContactSubmissionResponse(
                true,
                "Tin nhắn của bạn đã được gửi thành công. Chúng tôi sẽ liên hệ lại sớm nhất có thể.",
                "TKT-" + String.format("%06d", contact.getId())
        ));
    }
}
