package com.hsf.hotel.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** Standard response builders for controllers. */
public final class ApiResponses {
    private ApiResponses() {
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(Object id, T data) {
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.ok(data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> status(HttpStatus status, T data) {
        return ResponseEntity.status(status).body(ApiResponse.ok(data));
    }
}
