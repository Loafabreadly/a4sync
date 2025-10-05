package com.a4sync.common.version;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VersionInfo {
    private final String version;
    private final String commitId;
    private final String commitTime;
    private final String branch;

    private static VersionInfo instance;

    @JsonCreator
    public VersionInfo(
            @JsonProperty("version") String version,
            @JsonProperty("commitId") String commitId,
            @JsonProperty("commitTime") String commitTime,
            @JsonProperty("branch") String branch) {
        this.version = version;
        this.commitId = commitId;
        this.commitTime = commitTime;
        this.branch = branch;
    }

    private static VersionInfo createInstance() {
        Properties gitProps = new Properties();
        try (InputStream is = VersionInfo.class.getClassLoader().getResourceAsStream("git.properties")) {
            if (is != null) {
                gitProps.load(is);
            }
        } catch (IOException e) {
            // If we can't load the properties, we'll use default values
        }

        String version = getImplementationVersion();
        String commitId = gitProps.getProperty("git.commit.id.abbrev", "unknown");
        String commitTime = gitProps.getProperty("git.commit.time", "unknown");
        String branch = gitProps.getProperty("git.branch", "unknown");
        
        return new VersionInfo(version, commitId, commitTime, branch);
    }

    public static synchronized VersionInfo getInstance() {
        if (instance == null) {
            instance = createInstance();
        }
        return instance;
    }

    private static String getImplementationVersion() {
        String version = VersionInfo.class.getPackage().getImplementationVersion();
        return version != null ? version : "development";
    }

    @JsonProperty
    public String getVersion() {
        return version;
    }

    @JsonProperty
    public String getCommitId() {
        return commitId;
    }

    @JsonProperty
    public String getCommitTime() {
        return commitTime;
    }

    @JsonProperty
    public String getBranch() {
        return branch;
    }

    /**
     * Checks version compatibility with another VersionInfo instance.
     * @param other The other VersionInfo to compare with
     * @return A message if versions are different, null if they match
     */
    public String checkCompatibility(VersionInfo other) {
        return switch(other) {
            case null -> "Unable to determine server version";
            case VersionInfo v when !version.equals(v.version) || !commitId.equals(v.commitId) ->
                """
                Version mismatch detected:
                Client: %s (commit: %s)
                Server: %s (commit: %s)
                """.formatted(version, commitId, v.version, v.commitId);
            default -> null;
        };
    }

    @Override
    public String toString() {
        return String.format("Version: %s (commit: %s, branch: %s, time: %s)",
                version, commitId, branch, commitTime);
    }
}