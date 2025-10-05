package com.a4sync.server.controller;

import com.a4sync.common.version.VersionInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/version")
public class VersionController {

    private final VersionInfo versionInfo;

    public VersionController() {
        this.versionInfo = VersionInfo.getInstance();
    }

    @GetMapping
    public VersionInfo getVersion() {
        return versionInfo;
    }
}