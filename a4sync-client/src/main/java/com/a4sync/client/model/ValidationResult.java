package com.a4sync.client.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of mod validation operation
 */
@Data
public class ValidationResult {
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;
    private final ModInfo modInfo;
    
    public ValidationResult(boolean valid, List<String> errors, List<String> warnings, ModInfo modInfo) {
        this.valid = valid;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.warnings = warnings != null ? warnings : new ArrayList<>();
        this.modInfo = modInfo;
    }

}