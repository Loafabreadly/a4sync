package com.a4sync.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModSet {
    private String name;
    private String description;
    private String version;
    private List<Mod> mods = new ArrayList<>();
    private GameOptions gameOptions = new GameOptions();
    private List<String> dlcRequired = new ArrayList<>();
    private long totalSize;
}

