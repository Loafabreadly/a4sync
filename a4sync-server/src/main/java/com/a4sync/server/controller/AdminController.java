package com.a4sync.server.controller;

import com.a4sync.common.model.ModIndex;
import com.a4sync.common.model.ModSet;
import com.a4sync.server.service.ModManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Mod management endpoints")
public class AdminController {
    
    private final ModManagementService modManagementService;
    
    @PostMapping("/mods/scan")
    @Operation(summary = "Scan for mods", description = "Scan the mod directory and create/update mod.json files")
    public ResponseEntity<String> scanMods() {
        int count = modManagementService.scanAndUpdateMods();
        return ResponseEntity.ok("Successfully scanned and updated %d mods".formatted(count));
    }
    
    @PostMapping("/modsets")
    @Operation(summary = "Create mod set", description = "Create a new mod set configuration")
    public ResponseEntity<ModSet> createModSet(@RequestBody ModSet modSet) {
        return ResponseEntity.ok(modManagementService.createModSet(modSet));
    }
    
    @PutMapping("/mods/{modName}/index")
    @Operation(summary = "Update mod index", description = "Update a mod's index file")
    public ResponseEntity<ModIndex> updateModIndex(@PathVariable String modName) {
        return ResponseEntity.ok(modManagementService.updateModIndex(modName));
    }
}