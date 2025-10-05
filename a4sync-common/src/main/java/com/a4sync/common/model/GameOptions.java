package com.a4sync.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameOptions {
    private String profileName;
    private boolean noSplash;
    private String additionalParameters;
    private List<String> modDirectories = new ArrayList<>();
}
