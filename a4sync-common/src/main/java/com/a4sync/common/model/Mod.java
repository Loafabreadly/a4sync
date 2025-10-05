package com.a4sync.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mod {
    private String name;
    private String version;
    private String downloadUrl;
    private String hash;
    private long size;
}
