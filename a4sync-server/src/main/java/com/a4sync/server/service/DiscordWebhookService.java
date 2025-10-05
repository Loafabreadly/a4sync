package com.a4sync.server.service;

import com.a4sync.common.model.ModSet;
import com.a4sync.server.config.DiscordProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordWebhookService {
    
    private final DiscordProperties discordProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    
    public DiscordWebhookService(DiscordProperties discordProperties, ObjectMapper objectMapper) {
        this.discordProperties = discordProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
    
    /**
     * Send notification for modset update
     * 
     * @param modSet The updated modset
     * @param previousVersion Previous version info for comparison (can be null for new modsets)
     */
    public void sendModsetUpdateNotification(ModSet modSet, ModSetVersionInfo previousVersion) {
        if (!discordProperties.isEnabled() || !StringUtils.hasText(discordProperties.getWebhookUrl())) {
            log.debug("Discord notifications disabled or webhook URL not configured");
            return;
        }
        
        try {
            Map<String, Object> webhookPayload = createWebhookPayload(modSet, previousVersion);
            String jsonPayload = objectMapper.writeValueAsString(webhookPayload);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(discordProperties.getWebhookUrl()))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "A4Sync-Server/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Successfully sent Discord notification for modset update: {}", modSet.getName());
            } else {
                log.warn("Discord webhook returned status {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to send Discord webhook notification for modset: {}", modSet.getName(), e);
        }
    }
    
    private Map<String, Object> createWebhookPayload(ModSet modSet, ModSetVersionInfo previousVersion) {
        Map<String, Object> payload = new HashMap<>();
        
        // Set username and avatar
        if (StringUtils.hasText(discordProperties.getUsername())) {
            payload.put("username", discordProperties.getUsername());
        }
        if (StringUtils.hasText(discordProperties.getAvatarUrl())) {
            payload.put("avatar_url", discordProperties.getAvatarUrl());
        }
        
        // Add mention if configured
        String content = "";
        if (discordProperties.isMentionEveryone()) {
            content = "@everyone ";
        } else if (StringUtils.hasText(discordProperties.getCustomMention())) {
            content = discordProperties.getCustomMention() + " ";
        }
        
        if (!content.isEmpty()) {
            payload.put("content", content);
        }
        
        // Create embed
        List<Map<String, Object>> embeds = new ArrayList<>();
        Map<String, Object> embed = createModsetEmbed(modSet, previousVersion);
        embeds.add(embed);
        payload.put("embeds", embeds);
        
        return payload;
    }
    
    private Map<String, Object> createModsetEmbed(ModSet modSet, ModSetVersionInfo previousVersion) {
        Map<String, Object> embed = new HashMap<>();
        
        // Set embed color
        embed.put("color", discordProperties.getEmbedColor());
        
        // Set title and description
        boolean isNewModset = previousVersion == null;
        String title = isNewModset ? 
            "🆕 New Modset Available: " + modSet.getName() :
            "🔄 Modset Updated: " + modSet.getName();
        embed.put("title", title);
        
        // Calculate total size in GB
        long totalBytes = modSet.getMods().stream()
                .mapToLong(mod -> mod.getSize())
                .sum();
        BigDecimal totalSizeGB = BigDecimal.valueOf(totalBytes)
                .divide(BigDecimal.valueOf(1024 * 1024 * 1024), 2, RoundingMode.HALF_UP);
        
        StringBuilder description = new StringBuilder();
        description.append("**Version:** ").append(modSet.getVersion()).append("\n");
        description.append("**Total Size:** ").append(totalSizeGB).append(" GB\n");
        description.append("**Mod Count:** ").append(modSet.getMods().size()).append(" mods\n");
        
        // Add size comparison if previous version exists
        if (!isNewModset) {
            BigDecimal previousSizeGB = BigDecimal.valueOf(previousVersion.getTotalSize())
                    .divide(BigDecimal.valueOf(1024 * 1024 * 1024), 2, RoundingMode.HALF_UP);
            BigDecimal sizeDifference = totalSizeGB.subtract(previousSizeGB);
            
            description.append("**Previous Size:** ").append(previousSizeGB).append(" GB\n");
            
            if (sizeDifference.compareTo(BigDecimal.ZERO) > 0) {
                description.append("**Size Change:** +").append(sizeDifference.abs()).append(" GB ⬆️\n");
            } else if (sizeDifference.compareTo(BigDecimal.ZERO) < 0) {
                description.append("**Size Change:** -").append(sizeDifference.abs()).append(" GB ⬇️\n");
            } else {
                description.append("**Size Change:** No change\n");
            }
            
            // Add mod count comparison
            int modDifference = modSet.getMods().size() - previousVersion.getModCount();
            if (modDifference > 0) {
                description.append("**Mod Changes:** +").append(modDifference).append(" mods ⬆️\n");
            } else if (modDifference < 0) {
                description.append("**Mod Changes:** ").append(modDifference).append(" mods ⬇️\n");
            }
        }
        
        embed.put("description", description.toString());
        
        // Add thumbnail if enabled
        if (discordProperties.isIncludeThumbnail() && StringUtils.hasText(discordProperties.getThumbnailUrl())) {
            Map<String, Object> thumbnail = new HashMap<>();
            thumbnail.put("url", discordProperties.getThumbnailUrl());
            embed.put("thumbnail", thumbnail);
        }
        
        // Add fields for additional information
        List<Map<String, Object>> fields = new ArrayList<>();
        
        // Add download info field
        Map<String, Object> downloadField = new HashMap<>();
        downloadField.put("name", "📥 Download Instructions");
        downloadField.put("value", "Use your A4Sync client to download this modset:\n`Connect to server → Select '" + modSet.getName() + "'`");
        downloadField.put("inline", false);
        fields.add(downloadField);
        
        embed.put("fields", fields);
        
        // Add timestamp
        embed.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        
        // Add footer
        Map<String, Object> footer = new HashMap<>();
        footer.put("text", "A4Sync Server • Modset Update");
        embed.put("footer", footer);
        
        return embed;
    }
    
    /**
     * Helper class to store previous version information for comparison
     */
    public static class ModSetVersionInfo {
        private final String version;
        private final long totalSize;
        private final int modCount;
        
        public ModSetVersionInfo(String version, long totalSize, int modCount) {
            this.version = version;
            this.totalSize = totalSize;
            this.modCount = modCount;
        }
        
        public String getVersion() { return version; }
        public long getTotalSize() { return totalSize; }
        public int getModCount() { return modCount; }
    }
}