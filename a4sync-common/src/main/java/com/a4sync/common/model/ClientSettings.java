package com.a4sync.common.model;

import lombok.Data;

/**
 * Client settings configuration
 */
@Data
public class ClientSettings {
    private DownloadSettings download;
    private SecuritySettings security;
    private UISettings ui;

}