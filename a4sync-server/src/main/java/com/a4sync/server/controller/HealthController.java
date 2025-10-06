package com.a4sync.server.controller;

import com.a4sync.server.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Server health check endpoints")
public class HealthController {
    private final HealthService healthService;
    
    @GetMapping("/health")
    @Operation(summary = "Server health check", description = "Checks server health status including file system accessibility and basic service availability")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Health check completed (check status field for UP/DOWN/DEGRADED)")
    })
    public ResponseEntity<Status> checkHealth() {
        try {
            HealthService.HealthStatus status = healthService.checkHealth();
            return ResponseEntity.ok(new Status(status.name()));
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