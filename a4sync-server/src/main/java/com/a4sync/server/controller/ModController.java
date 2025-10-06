package com.a4sync.server.controller;

import com.a4sync.common.model.A4SyncConfig;
import com.a4sync.common.model.ModSet;
import com.a4sync.common.model.RepositoryInfo;
import com.a4sync.server.resource.RangeResource;
import com.a4sync.server.service.ModSetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Mod Management", description = "Endpoints for managing and downloading mods and mod sets")
public class ModController {
    
    private final ModSetService modSetService;

    public ModController(ModSetService modSetService) {
        this.modSetService = modSetService;
    }

    @GetMapping("/modsets")
    @Operation(summary = "Get all mod sets", description = "Retrieves a list of all available mod sets from the repository")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved mod sets"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ModSet>> getModSets() {
        try {
            List<ModSet> modSets = modSetService.getAllModSets();
            return ResponseEntity.ok(modSets);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/modsets/{name}")
    @Operation(summary = "Get specific mod set", description = "Retrieves details of a specific mod set by name")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved mod set"),
        @ApiResponse(responseCode = "404", description = "Mod set not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ModSet> getModSet(
            @Parameter(description = "Name of the mod set", required = true) 
            @PathVariable String name) {
        try {
            return modSetService.getModSet(name)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/modsets/{modSetName}/mods/{modName}")
    @Operation(summary = "Download mod file", description = "Downloads a specific mod file, supports HTTP Range requests for resumable downloads")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Full file download"),
        @ApiResponse(responseCode = "206", description = "Partial content (range request)"),
        @ApiResponse(responseCode = "404", description = "Mod file not found"),
        @ApiResponse(responseCode = "416", description = "Range not satisfiable")
    })
    public ResponseEntity<Resource> downloadMod(
            @Parameter(description = "Name of the mod set", required = true) 
            @PathVariable String modSetName,
            @Parameter(description = "Name of the mod file", required = true) 
            @PathVariable String modName,
            @Parameter(description = "HTTP Range header for resumable downloads", example = "bytes=0-1023") 
            @RequestHeader(value = "Range", required = false) String rangeHeader) {
        try {
            Path modPath = modSetService.getModPath(modSetName, modName);
            if (!Files.exists(modPath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(modPath.toUri());
            long contentLength = Files.size(modPath);

            // Handle range requests for partial downloads
            if (rangeHeader != null) {
                String[] ranges = rangeHeader.replace("bytes=", "").split("-");
                long start = Long.parseLong(ranges[0]);
                long end = ranges.length > 1 ? Long.parseLong(ranges[1]) : contentLength - 1;
                long rangeLength = end - start + 1;

                return ResponseEntity.status(206)
                        .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .header(HttpHeaders.CONTENT_RANGE, "bytes %d-%d/%d".formatted(start, end, contentLength))
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(rangeLength))
                        .body(new RangeResource(resource, start, rangeLength));
            }

            // Normal full download
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + modName + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/autoconfig")
    @Operation(summary = "Get auto-configuration", description = "Generates automatic mod set configuration for legacy client compatibility")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully generated auto-configuration"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ModSet> getAutoConfig() {
        try {
            ModSet autoConfig = modSetService.generateAutoConfig();
            return ResponseEntity.ok(autoConfig);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/repository/info")
    @Operation(summary = "Get repository information", description = "Provides comprehensive repository metadata including mod sets count, sizes, and last updated times")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved repository information"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<RepositoryInfo> getRepositoryInfo() {
        try {
            RepositoryInfo repositoryInfo = modSetService.generateRepositoryInfo();
            return ResponseEntity.ok(repositoryInfo);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/repository/size")
    @Operation(summary = "Get repository total size", description = "Returns the total size in bytes of all mod files in the repository")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully calculated repository size"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Long> getRepositorySize() {
        try {
            long totalSize = modSetService.calculateTotalRepositorySize();
            return ResponseEntity.ok(totalSize);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping(value = {"/a4sync.json", "/.a4sync", "/config"})
    @Operation(summary = "Get A4Sync configuration", description = "Returns repository configuration in A4Sync format for client auto-discovery")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully generated A4Sync configuration"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<A4SyncConfig> getA4SyncConfig(HttpServletRequest request) {
        try {
            A4SyncConfig config = modSetService.generateA4SyncConfig(request);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
