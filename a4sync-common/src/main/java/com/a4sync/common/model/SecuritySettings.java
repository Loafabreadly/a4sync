package com.a4sync.common.model;

import lombok.Data;

/**
 * Security-related settings
 */
@Data
public class SecuritySettings {
    private boolean validateSignatures = true;
    private boolean allowUnsignedMods = false;
    private String trustedKeysPath;
    private boolean checkModIntegrity = true;
}