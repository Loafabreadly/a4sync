package com.a4sync.common.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GameType {
    ARMA_3("Arma 3", "107410", "arma3_x64.exe"),
    ARMA_4("Arma 4", "2874680", "arma4.exe");
    
    private final String displayName;
    private final String steamAppId;
    private final String executableName;
    
    @Override
    public String toString() {
        return displayName;
    }
}