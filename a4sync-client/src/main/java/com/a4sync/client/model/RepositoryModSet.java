package com.a4sync.client.model;

import com.a4sync.common.model.ModSet;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class RepositoryModSet {
    private Repository repository;
    private ModSet modSet;
    
    public String getDisplayName() {
        if (repository == null) {
            return modSet.getName() + " [Local]";
        }
        return String.format("%s [%s]", modSet.getName(), repository.getDisplayName());
    }
}