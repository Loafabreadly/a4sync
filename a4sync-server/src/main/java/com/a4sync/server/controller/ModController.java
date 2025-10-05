package com.a4sync.server.controller;

import com.a4sync.common.model.A4SyncConfig;
import com.a4sync.common.model.ModSet;
import com.a4sync.common.model.RepositoryInfo;
import com.a4sync.server.resource.RangeResource;
import com.a4sync.server.service.ModSetService;
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
public class ModController {
    
    private final ModSetService modSetService;

    public ModController(ModSetService modSetService) {
        this.modSetService = modSetService;
    }

    @GetMapping("/modsets")
    public ResponseEntity<List<ModSet>> getModSets() {
        try {
            List<ModSet> modSets = modSetService.getAllModSets();
            return ResponseEntity.ok(modSets);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/modsets/{name}")
    public ResponseEntity<ModSet> getModSet(@PathVariable String name) {
        try {
            return modSetService.getModSet(name)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/modsets/{modSetName}/mods/{modName}")
    public ResponseEntity<Resource> downloadMod(
            @PathVariable String modSetName,
            @PathVariable String modName,
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
    public ResponseEntity<ModSet> getAutoConfig() {
        try {
            ModSet autoConfig = modSetService.generateAutoConfig();
            return ResponseEntity.ok(autoConfig);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/repository/info")
    public ResponseEntity<RepositoryInfo> getRepositoryInfo() {
        try {
            RepositoryInfo repositoryInfo = modSetService.generateRepositoryInfo();
            return ResponseEntity.ok(repositoryInfo);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping(value = {"/a4sync.json", "/.a4sync", "/config"})
    public ResponseEntity<A4SyncConfig> getA4SyncConfig(HttpServletRequest request) {
        try {
            A4SyncConfig config = modSetService.generateA4SyncConfig(request);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
