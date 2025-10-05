package com.a4sync.server.controller;

import com.a4sync.server.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthController {
    private final HealthService healthService;
    
    @GetMapping("/health")
    public ResponseEntity<Status> checkHealth() {
        try {
            healthService.checkHealth();
            return ResponseEntity.ok(new Status("UP"));
        } catch (Exception e) {
            return ResponseEntity.ok(new Status("DOWN", e.getMessage()));
        }
    }
    
    private record Status(String status, String message) {
        public Status(String status) {
            this(status, null);
        }
    }
}