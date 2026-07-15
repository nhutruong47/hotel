package com.hsf.hotel.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a domain object cannot be found. Mapped to HTTP 404.
 */
public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                String.format("%s with id %s was not found", resource, id));
    }

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }
}
