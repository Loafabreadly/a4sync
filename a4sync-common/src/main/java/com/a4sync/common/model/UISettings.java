package com.a4sync.common.model;

import lombok.Data;

/**
 * UI/UX preferences
 */
@Data
public class UISettings {
    private String theme = "default";
    private boolean showProgressDetails = true;
    private boolean enableNotifications = true;
    private String defaultGameLaunchProfile;
}