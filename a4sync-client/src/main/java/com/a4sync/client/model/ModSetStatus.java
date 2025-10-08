package com.a4sync.client.model;

import com.a4sync.client.model.Repository;
import com.a4sync.common.model.ModSet;

/**
 * Status information for mod set updates
 */
public class ModSetStatus {
    public enum Status {
        NOT_DOWNLOADED,
        UPDATE_AVAILABLE,
        UP_TO_DATE
    }
    
    private final Repository repository;
    private final ModSet remoteSet;
    private final ModSet localSet;
    private final Status status;
    
    private ModSetStatus(Repository repository, ModSet remoteSet, ModSet localSet, Status status) {
        this.repository = repository;
        this.remoteSet = remoteSet;
        this.localSet = localSet;
        this.status = status;
    }
    
    public Repository getRepository() {
        return repository;
    }
    
    public ModSet getRemoteSet() {
        return remoteSet;
    }
    
    public ModSet getLocalSet() {
        return localSet;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public static ModSetStatus notDownloaded(Repository repo, ModSet remoteSet) {
        return new ModSetStatus(repo, remoteSet, null, Status.NOT_DOWNLOADED);
    }
    
    public static ModSetStatus updateAvailable(Repository repo, ModSet remoteSet, ModSet localSet) {
        return new ModSetStatus(repo, remoteSet, localSet, Status.UPDATE_AVAILABLE);
    }
    
    public static ModSetStatus upToDate(Repository repo, ModSet remoteSet, ModSet localSet) {
        return new ModSetStatus(repo, remoteSet, localSet, Status.UP_TO_DATE);
    }
}