package com.a4sync.client.service;

import java.nio.file.Path;

public class RepositoryManagerFactory {
    private static RepositoryManager instance;
    
    public static synchronized RepositoryManager getInstance() {
        if (instance == null) {
            instance = new RepositoryManager();
        }
        return instance;
    }
    
    public static void initialize(Path appDirectory) {
        if (instance != null) {
            throw new IllegalStateException("RepositoryManager already initialized");
        }
        instance = new RepositoryManager();
    }
}