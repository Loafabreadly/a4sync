package com.a4sync.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties(prefix = "a4sync.discord")
public class DiscordProperties {
    /**
     * Enable or disable Discord webhook notifications
     */
    private boolean enabled = false;
    
    /**
     * Discord webhook URL for notifications
     */
    private String webhookUrl;
    
    /**
     * Username to display in Discord messages
     */
    private String username = "A4Sync Server";
    
    /**
     * Avatar URL for the Discord bot user
     */
    private String avatarUrl;
    
    /**
     * Color for the embed (in decimal format, e.g., 5814783 for #58B9FF)
     */
    private int embedColor = 5814783; // Default blue color
    
    /**
     * Whether to include a thumbnail image in the embed
     */
    private boolean includeThumbnail = true;
    
    /**
     * Thumbnail image URL (e.g., A4Sync logo)
     */
    private String thumbnailUrl;
    
    /**
     * Whether to mention @everyone or @here in notifications
     */
    private boolean mentionEveryone = false;
    
    /**
     * Custom mention text (e.g., "<@&role_id>" for role mentions)
     */
    private String customMention;
}