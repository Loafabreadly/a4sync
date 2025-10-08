package com.a4sync.server.controller;

import com.a4sync.common.version.VersionInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Version", description = "Server version information endpoints")
public class VersionController {

    private final VersionInfo versionInfo;

    public VersionController() {
        this.versionInfo = VersionInfo.getInstance();
    }

    @GetMapping("/version")
    @Operation(summary = "Get server version", description = "Returns version information and compatibility details for client-server communication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved version information")
    })
    public VersionInfo getVersion() {
        return versionInfo;
    }
}