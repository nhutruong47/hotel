package com.hsf.hotel.health.api;

import com.hsf.hotel.config.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@com.hsf.hotel.config.ApiController
@RequestMapping(com.hsf.hotel.config.ApiPaths.V1)
public class HealthApi {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<?>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "status", "UP",
                "message", "Spring Boot API is running"
        )));
    }
}
