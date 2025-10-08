package com.a4sync.client.controller;

/**
 * Main controller for the A4Sync client application.
 * 
 * This controller implements the desired repository connection flow:
 * 
 * REPOSITORY CONNECTION FLOW:
 * ===========================
 * When a new repository is added or connected to:
 * 
 * 1. HEALTH API CHECK (/api/v1/health)
 *    - Hit the health endpoint to determine repository state (UP/DEGRADED/DOWN)
 *    - Validate server accessibility and authentication
 *    - Update repository status in UI
 * 
 * 2. MODSETS API FETCH (/api/v1/repository/info and /api/v1/modsets)
 *    - Retrieve repository information including modset count and metadata
 *    - Get list of available modsets with basic information
 *    - Populate UI components with modset information for user selection
 * 
 * 3. MODSET DOWNLOAD FLOW (/api/v1/modsets/{name} and /api/v1/modsets/{modset}/mods/{mod})
 *    - When user selects a modset, fetch detailed modset information
 *    - Download individual mod files using the chunked download system
 *    - Provide real-time progress feedback and error handling
 * 
 * Key Methods:
 * - addRepository(): Implements steps 1-2 for new repositories
 * - connectToRepository(): Implements steps 1-2 for existing repositories  
 * - downloadSelectedModFromTable(): Implements step 3 for modset downloads
 * 
 * This ensures consistent API usage and proper separation of concerns between
 * health checking, repository discovery, and file downloads.
 */

import com.a4sync.client.config.ClientConfig;
import com.a4sync.client.model.Repository;
import com.a4sync.client.model.RepositoryModSet;
import com.a4sync.client.model.ModSetStatus;
import com.a4sync.client.service.GameLauncher;
import com.a4sync.client.service.ModManager;
import com.a4sync.client.service.ModSetDownloadService;
import com.a4sync.client.service.MultiRepositoryService;
import com.a4sync.client.service.RepositoryService;
import com.a4sync.client.model.DownloadResult;
import com.a4sync.common.model.GameOptions;
import com.a4sync.common.model.GameType;
import com.a4sync.common.model.Mod;
import com.a4sync.common.model.ModSet;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import com.a4sync.client.model.HealthStatus;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.scene.layout.GridPane;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MainController {
    @FXML private TabPane mainTabPane;
    @FXML private ListView<RepositoryModSet> modSetList;
    @FXML private ComboBox<Repository> repositoryComboBox;
    @FXML private TextField modSetName;
    @FXML private ListView<String> modList;
    @FXML private TableView<Mod> availableModsTable;
    @FXML private TableColumn<Mod, String> modNameColumn;
    @FXML private TableColumn<Mod, String> modVersionColumn;
    @FXML private TableColumn<Mod, String> modSizeColumn;
    @FXML private TableColumn<Mod, String> modStatusColumn;
    @FXML private ProgressBar downloadProgress;
    @FXML private Label statusLabel;
    

    
    // Repository status tab components
    @FXML private TableView<Repository> repositoryStatusTable;
    @FXML private TableColumn<Repository, String> repoNameColumn;
    @FXML private TableColumn<Repository, String> repoUrlColumn;
    @FXML private TableColumn<Repository, String> repoStatusColumn;
    @FXML private TableColumn<Repository, String> repoModCountColumn;
    @FXML private TableColumn<Repository, String> repoModSetCountColumn;
    @FXML private TableColumn<Repository, String> repoSizeColumn;
    @FXML private TableColumn<Repository, String> repoLastUpdatedColumn;
    @FXML private TextArea repositoryDetailsArea;
    

    
    private final ClientConfig config;
    private final ModManager modManager;
    private final GameLauncher gameLauncher;
    private final MultiRepositoryService multiRepositoryService;
    private final ObservableList<RepositoryModSet> modSets;
    private final ObservableList<Mod> availableMods;
    private final ObservableList<Repository> repositories;

    
    public MainController() {
        this.config = ClientConfig.loadConfig();
        this.config.migrateLegacyRepository(); // Handle backward compatibility
        this.modManager = new ModManager(config);
        this.gameLauncher = new GameLauncher(config);
        this.multiRepositoryService = new MultiRepositoryService(config);
        this.modSets = FXCollections.observableArrayList();
        this.availableMods = FXCollections.observableArrayList();
        this.repositories = FXCollections.observableArrayList();

    }

    @FXML
    private void initialize() {
        modSetList.setItems(modSets);
        availableModsTable.setItems(availableMods);
        repositoryComboBox.setItems(repositories);
            
        // Setup table columns (used for both mods and modsets)
        modNameColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getName()));
        modVersionColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getVersion()));
        modSizeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getSize() > 0 ? formatSize(cellData.getValue().getSize()) : "Mod Set"));
        modStatusColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getSize() > 0 ? 
                (modManager.isModInstalled(cellData.getValue()) ? "Installed" : "Download") : "Select"));
            
        
        
        // Setup repository status table columns
        if (repositoryStatusTable != null) {
            repositoryStatusTable.setItems(repositories);
            
            repoNameColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getName()));
            repoUrlColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getUrl()));
            repoStatusColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(getRepositoryStatusText(cellData.getValue())));
            repoModCountColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(String.valueOf(cellData.getValue().getModCount())));
            repoModSetCountColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(String.valueOf(cellData.getValue().getModSetCount())));
            repoSizeColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(formatSize(cellData.getValue().getTotalSize())));
            repoLastUpdatedColumn.setCellValueFactory(cellData -> {
                Repository repo = cellData.getValue();
                return new SimpleStringProperty(repo.getLastUpdated() != null ? 
                    repo.getLastUpdated().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "Unknown");
            });
            
            repositoryStatusTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldRepo, newRepo) -> {
                    updateRepositoryStatusDetails(newRepo);
                });
        }
            
        // Add selection listeners
        modSetList.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> updateModSetDetails(newValue));
            
        // Setup repository display
        repositoryComboBox.setCellFactory(listView -> new ListCell<Repository>() {
            @Override
            protected void updateItem(Repository repo, boolean empty) {
                super.updateItem(repo, empty);
                setText(empty || repo == null ? null : repo.getDisplayName());
            }
        });
        repositoryComboBox.setButtonCell(new ListCell<Repository>() {
            @Override
            protected void updateItem(Repository repo, boolean empty) {
                super.updateItem(repo, empty);
                setText(empty || repo == null ? null : repo.getDisplayName());
            }
        });
        
        // Setup mod set display
        modSetList.setCellFactory(listView -> new ListCell<RepositoryModSet>() {
            @Override
            protected void updateItem(RepositoryModSet item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });
        
        // Load repositories
        loadRepositories();
    }
    
    private void updateModSetDetails(RepositoryModSet repositoryModSet) {
        if (repositoryModSet != null) {
            ModSet modSet = repositoryModSet.getModSet();
            modSetName.setText(modSet.getName());
            
            modList.getItems().clear();
            if (modSet.getMods() != null) {
                modList.getItems().addAll(
                    modSet.getMods().stream()
                        .map(Mod::getName)
                        .toList()
                );
            }
        } else {
            modSetName.clear();
            modList.getItems().clear();
        }
    }
    
    private void loadRepositories() {
        repositories.clear();
        repositories.addAll(config.getRepositories());
        if (!repositories.isEmpty()) {
            repositoryComboBox.getSelectionModel().selectFirst();
        }
    }
    
    @FXML
    protected void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About A4Sync");
        alert.setHeaderText("A4Sync Client");
        alert.setContentText(com.a4sync.common.version.VersionInfo.getInstance().toString());
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait();
    }
    
    @FXML
    private void addRepository() {
        Dialog<Repository> dialog = new Dialog<>();
        dialog.setTitle("Add Repository");
        dialog.setHeaderText("Enter repository details");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        TextField nameField = new TextField();
        nameField.setPromptText("Repository name");
        TextField urlField = new TextField();
        urlField.setPromptText("https://your-repo.com");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Optional password");
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("URL:"), 0, 1);
        grid.add(urlField, 1, 1);
        grid.add(new Label("Password:"), 0, 2);
        grid.add(passwordField, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Enable/disable OK button based on input
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        
        nameField.textProperty().addListener((obs, oldText, newText) -> 
            okButton.setDisable(newText.trim().isEmpty() || urlField.getText().trim().isEmpty()));
        urlField.textProperty().addListener((obs, oldText, newText) -> 
            okButton.setDisable(newText.trim().isEmpty() || nameField.getText().trim().isEmpty()));
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return new Repository(nameField.getText().trim(), urlField.getText().trim());
            }
            return null;
        });
        
        Optional<Repository> result = dialog.showAndWait();
        if (result.isPresent()) {
            Repository newRepo = result.get();
            
            // Show progress while following the repository connection flow
            statusLabel.setText("Step 1: Testing connection to " + newRepo.getName() + "...");
            downloadProgress.setProgress(-1.0); // Indeterminate progress
            
            // Use the proper repository connection flow via MultiRepositoryService
            multiRepositoryService.addRepositoryWithValidation(newRepo).thenAccept(validatedRepo -> {
                Platform.runLater(() -> {
                    // Repository successfully validated and added
                    config.saveConfig(); // Save the configuration to persist the repository
                    loadRepositories();
                    repositoryComboBox.getSelectionModel().select(validatedRepo);
                    
                    statusLabel.setText("✓ Repository '" + validatedRepo.getName() + "' added successfully!");
                    downloadProgress.setProgress(0);
                    
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Repository Added");
                    success.setHeaderText("Connection Flow Completed Successfully!");
                    success.setContentText("Repository '" + validatedRepo.getName() + "' has been added!\n\n" +
                                          "Connection Flow Results:\n" +
                                          "✓ Step 1: Health API check - " + validatedRepo.getHealthStatus().name() + "\n" +
                                          "✓ Step 2: Modsets API fetch - " + validatedRepo.getModSetCount() + " modsets found\n" +
                                          "✓ Step 3: Repository populated with server data\n\n" +
                                          "Repository Details:\n" +
                                          "• Name: " + validatedRepo.getName() + "\n" +
                                          "• Available Modsets: " + validatedRepo.getModSetCount() + "\n" +
                                          "• Last Updated: " + validatedRepo.getLastUpdated().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\n\n" +
                                          "You can now connect to this repository to download modsets.");
                    success.showAndWait();
                });
            }).exceptionally(throwable -> {
                Platform.runLater(() -> {
                    newRepo.setHealthStatus(HealthStatus.ERROR);
                    newRepo.setLastChecked(LocalDateTime.now());
                    statusLabel.setText("✗ Repository connection flow failed");
                    downloadProgress.setProgress(0);
                    
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Connection Failed");
                    confirm.setHeaderText("Repository Connection Flow Failed");
                    confirm.setContentText("Failed to complete connection flow for " + newRepo.getName() + 
                                          "\n\nConnection Flow Status:\n" +
                                          (throwable.getMessage().contains("health") ? "✗" : "✓") + " Step 1: Health API check\n" +
                                          (throwable.getMessage().contains("health") ? "-" : "✗") + " Step 2: Modsets API fetch\n" +
                                          "- Step 3: Repository population\n\n" +
                                          "Error: " + throwable.getMessage() + 
                                          "\n\nThe repository may be offline or inaccessible. Add it anyway?");
                    
                    Optional<ButtonType> confirmResult = confirm.showAndWait();
                    if (confirmResult.isPresent() && confirmResult.get() == ButtonType.OK) {
                        multiRepositoryService.addRepository(newRepo);
                        config.saveConfig(); // Save the configuration to persist the repository
                        loadRepositories();
                        repositoryComboBox.getSelectionModel().select(newRepo);
                        statusLabel.setText("Repository '" + newRepo.getName() + "' added (offline)");
                    }
                });
                return null;
            });
        }
    }
    
    @FXML
    private void refreshAllRepositories() {
        if (repositories.isEmpty()) {
            showInfo("No Repositories", "No repositories configured to refresh.");
            return;
        }

        // Show progress indicator
        showInfo("Refreshing", "Testing connections for all repositories...");
        
        // Test all repositories in background thread to avoid blocking UI
        Task<Void> refreshTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int total = repositories.size();
                int current = 0;
                
                for (Repository repo : repositories) {
                    current++;
                    updateProgress(current, total);
                    updateMessage("Testing " + repo.getName() + "...");
                    
                    // Test repository connection
                    try {
                        // Create temporary config for this specific repository
                        ClientConfig tempConfig = new ClientConfig();
                        tempConfig.setServerUrl(repo.getUrl());
                        tempConfig.setRepositoryPassword(repo.getPassword());
                        tempConfig.setUseAuthentication(repo.isUseAuthentication());
                        
                        RepositoryService repoService = new RepositoryService(tempConfig);
                        
                        // Use the async method and wait for result
                        HealthStatus healthStatus = repoService.testConnectionAsync().get();
                        Platform.runLater(() -> {
                            repo.setHealthStatus(healthStatus);
                            repo.setLastChecked(LocalDateTime.now());
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            repo.setHealthStatus(HealthStatus.ERROR);
                            repo.setLastChecked(LocalDateTime.now());
                        });
                    }
                    
                    // Small delay to prevent overwhelming servers
                    Thread.sleep(100);
                }
                return null;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    repositoryStatusTable.refresh();
                    showInfo("Refresh Complete", "All repository connections have been tested.");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showError("Refresh Failed", "An error occurred while refreshing repositories: " + 
                             getException().getMessage());
                });
            }
        };
        
        Thread refreshThread = new Thread(refreshTask);
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    @FXML
    private void testSelectedRepository() {
        testSelectedRepositoryFromStatus(); // Use the more efficient async version
    }



    @FXML
    private void connectToRepository() {
        Repository selectedRepo = repositoryStatusTable.getSelectionModel().getSelectedItem();
        if (selectedRepo == null) {
            showError("No Repository Selected", "Please select a repository to connect to.");
            return;
        }

        // Create a repository service for this specific repository
        RepositoryService repositoryService = new RepositoryService(config);
        repositoryService.setRepositoryUrl(selectedRepo.getUrl());

        // Follow desired repository connection flow:
        // 1. Hit health API endpoint to determine repository state
        // 2. Hit modsets endpoint to get repository details and modset information  
        // 3. Populate UI components with modsets available for download

        statusLabel.setText("Step 1: Checking repository health...");
        downloadProgress.setProgress(-1.0); // Indeterminate progress
        
        // Step 1: Hit the health API endpoint to determine repository state
        repositoryService.testConnectionAsync().thenCompose(healthStatus -> {
            if (healthStatus == HealthStatus.ERROR) {
                throw new RuntimeException("Repository health check failed - server may be down or unreachable");
            }
            
            Platform.runLater(() -> {
                selectedRepo.setHealthStatus(healthStatus);
                selectedRepo.setLastChecked(LocalDateTime.now());
                statusLabel.setText("Step 2: Repository is " + healthStatus.name().toLowerCase() + ", fetching modsets...");
            });
            
            // Step 2: Hit modsets endpoint to determine available modsets and repository details
            return repositoryService.getRepositoryInfo();
        }).thenAccept(repositoryInfo -> {
            Platform.runLater(() -> {
                statusLabel.setText("Step 3: Populating modsets in client UI...");
                
                // Step 3: Update repository with comprehensive info from server
                selectedRepo.setHealthStatus(HealthStatus.HEALTHY);
                selectedRepo.setLastChecked(LocalDateTime.now());
                selectedRepo.setName(repositoryInfo.getName()); // Use server's repository name
                int modSetCount = repositoryInfo.getModSets() != null ? repositoryInfo.getModSets().size() : 0;
                selectedRepo.setModSetCount(modSetCount);
                selectedRepo.setLastUpdated(repositoryInfo.getLastUpdated());
                
                // Clear available mods table and populate with modsets available for download
                availableMods.clear();
                
                // Populate UI with modset information for user selection
                if (repositoryInfo.getModSets() != null) {
                    repositoryInfo.getModSets().forEach(modSet -> {
                        // Create a pseudo-mod entry to display modset info in the table
                        com.a4sync.common.model.Mod modSetEntry = new com.a4sync.common.model.Mod();
                        modSetEntry.setName(modSet.getName());
                        modSetEntry.setVersion(modSet.getVersion());
                        modSetEntry.setSize(0L); // Size not needed for selection
                        availableMods.add(modSetEntry);
                    });
                }

                repositoryStatusTable.refresh();
                statusLabel.setText("✓ Connection complete: " + selectedRepo.getName() + " - " + 
                                  modSetCount + " modsets ready for download");
                downloadProgress.setProgress(0);
                
                showInfo("Repository Connected", "Successfully connected to " + selectedRepo.getName() + 
                        "\n\nConnection Flow Completed:" +
                        "\n✓ Health check: " + selectedRepo.getHealthStatus().name() +
                        "\n✓ Repository info: Retrieved" +
                        "\n✓ Modsets loaded: " + modSetCount + " available" +
                        "\n\nRepository Details:" +
                        "\n• Name: " + repositoryInfo.getName() +
                        "\n• Last Updated: " + repositoryInfo.getLastUpdated().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) +
                        "\n\nNext: Select a modset from the table to download its mods.");
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                selectedRepo.setHealthStatus(HealthStatus.ERROR);
                selectedRepo.setLastChecked(LocalDateTime.now());
                repositoryStatusTable.refresh();
                statusLabel.setText("✗ Connection failed at step " + 
                    (throwable.getMessage().contains("health") ? "1 (health check)" : "2 (modsets fetch)"));
                downloadProgress.setProgress(0);
                showError("Connection Failed", "Failed to connect to " + selectedRepo.getName() + 
                        "\n\nConnection Flow:" +
                        "\n" + (throwable.getMessage().contains("health") ? "✗" : "✓") + " Step 1: Health API check" +
                        "\n" + (throwable.getMessage().contains("health") ? "-" : "✗") + " Step 2: Modsets API fetch" +
                        "\n- Step 3: UI population" +
                        "\n\nError Details: " + throwable.getMessage() + 
                        "\n\nPlease verify the server is running and accessible at: " + selectedRepo.getUrl());
            });
            return null;
        });
    }

    @FXML
    private void refreshUpdates() {
        showInfo("Feature Coming Soon", "Update checking functionality will be implemented in the future.");
    }
    
    // Enhanced update checking classes and methods
    private enum UpdateStatus {
        NOT_DOWNLOADED,
        UPDATE_AVAILABLE, 
        UP_TO_DATE,
        LOCAL_NEWER
    }
    
    private static class ModSetUpdateInfo {
        String repositoryName;
        String modSetName;
        String localVersion;
        String remoteVersion;
        long remoteSize;
        LocalDateTime lastUpdated;
        UpdateStatus updateStatus;
        
        public String getStatusDescription() {
            switch (updateStatus) {
                case NOT_DOWNLOADED: return "Available for Download";
                case UPDATE_AVAILABLE: return "Update Available";
                case UP_TO_DATE: return "Up to Date"; 
                case LOCAL_NEWER: return "Local Version Newer";
                default: return "Unknown";
            }
        }
    }
    
    private static class UpdateCheckResult {
        List<ModSetUpdateInfo> modSetUpdates = new ArrayList<>();
        int notDownloaded = 0;
        int updatesAvailable = 0;
        int upToDate = 0;
        int localNewer = 0;
    }


    
    private RepositoryModSet findLocalModSet(String modSetName) {
        return modSets.stream()
            .filter(repoModSet -> repoModSet.getModSet().getName().equals(modSetName))
            .findFirst()
            .orElse(null);
    }
    
    private int compareVersions(String version1, String version2) {
        if (version1 == null) version1 = "0.0.0";
        if (version2 == null) version2 = "0.0.0";
        
        // Handle special cases
        if (version1.equals(version2)) return 0;
        if (version1.equals("Unknown") || version1.isEmpty()) return -1;
        if (version2.equals("Unknown") || version2.isEmpty()) return 1;
        
        // Try semantic versioning comparison
        try {
            String[] v1Parts = version1.split("\\.");
            String[] v2Parts = version2.split("\\.");
            
            int maxLength = Math.max(v1Parts.length, v2Parts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int v1Part = i < v1Parts.length ? parseVersionPart(v1Parts[i]) : 0;
                int v2Part = i < v2Parts.length ? parseVersionPart(v2Parts[i]) : 0;
                
                if (v1Part != v2Part) {
                    return Integer.compare(v1Part, v2Part);
                }
            }
            
            return 0;
        } catch (Exception e) {
            // Fall back to string comparison
            return version1.compareTo(version2);
        }
    }
    
    private int parseVersionPart(String versionPart) {
        // Extract numeric part, ignoring alpha suffixes
        String numericPart = versionPart.replaceAll("[^0-9].*", "");
        return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
    }
    


    @FXML
    private void updateSelectedModSets() {
        // Check if we have a selected mod set from available mods table
        Mod selectedMod = availableModsTable.getSelectionModel().getSelectedItem();
        if (selectedMod != null) {
            downloadSelectedModFromTable(selectedMod);
            return;
        }
        
        showError("No Selection", "Please select a mod set to download from the Available Mod Sets table.");
    }
    
    private void downloadSelectedModSetUpdate(ModSetStatus modSetStatus) {
        String modSetName = modSetStatus.getRemoteSet() != null ? modSetStatus.getRemoteSet().getName() : "Unknown ModSet";
        showInfo("Download Starting", "Starting download of " + modSetName);
        
        // Use the existing repository manager to download
        Task<Void> downloadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Downloading " + modSetName + "...");
                
                try {
                    // Use MultiRepositoryService to download the mod set 
                    // This is a placeholder - actual implementation would need proper repository lookup
                    statusLabel.setText("Download functionality requires full integration with repository system");
                    
                } catch (Exception e) {
                    throw e;
                }
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    downloadProgress.setProgress(1.0);
                    statusLabel.setText("Download completed: " + modSetName);
                    showInfo("Download Complete", "Successfully downloaded " + modSetName);
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    downloadProgress.setProgress(0);
                    statusLabel.setText("Download failed: " + modSetName);
                    showError("Download Failed", "Failed to download " + modSetName + 
                             ": " + getException().getMessage());
                });
            }
        };
        
        // Bind progress bar to task
        downloadProgress.progressProperty().bind(downloadTask.progressProperty());
        statusLabel.textProperty().bind(downloadTask.messageProperty());
        
        Thread downloadThread = new Thread(downloadTask);
        downloadThread.setDaemon(true);
        downloadThread.start();
    }
    
    private void downloadSelectedModFromTable(Mod selectedMod) {
        // Get the currently connected repository
        Repository connectedRepo = repositories.stream()
            .filter(repo -> repo.getHealthStatus() == HealthStatus.HEALTHY)
            .findFirst()
            .orElse(null);
            
        if (connectedRepo == null) {
            showError("No Connected Repository", "Please connect to a repository first by selecting one and clicking 'Connect'.");
            return;
        }
        
        // The selected "mod" is actually a modset entry (from our UI population in connectToRepository)
        String modSetName = selectedMod.getName();
        
        statusLabel.setText("Preparing to download modset: " + modSetName);
        downloadProgress.setProgress(-1.0); // Indeterminate progress
        
        // Create repository service for the connected repository
        RepositoryService repositoryService = new RepositoryService(config);
        repositoryService.setRepositoryUrl(connectedRepo.getUrl());
        
        showInfo("Modset Download Starting", "Starting download of modset '" + modSetName + 
                "' from " + connectedRepo.getName() + 
                "\n\nThis will follow the proper download flow:\n" +
                "1. ✓ Health check (already completed)\n" +
                "2. Fetch detailed modset information\n" +
                "3. Download individual mod files from the modset");
        
        // Follow the desired modset download flow:
        // Step 1: Health check already completed (repository is connected)
        // Step 2: Get specific modset details to see what mods need to be downloaded
        // Step 3: Download individual mod files from the modset
        
        statusLabel.setText("Step 2: Fetching modset details for " + modSetName + "...");
        
        repositoryService.getSpecificModSet(modSetName).thenAccept(modSet -> {
            Platform.runLater(() -> {
                statusLabel.setText("Step 3: Starting download of " + modSet.getMods().size() + " mods...");
                
                // Now we have the complete modset with all mods, initiate the download
                ModSetDownloadService downloadService = new ModSetDownloadService(modManager, config);
                
                CompletableFuture<DownloadResult> downloadFuture = downloadService.downloadModSet(
                    modSet, 
                    connectedRepo.getUrl(),
                    progress -> {
                        // Update UI with download progress
                        Platform.runLater(() -> {
                            double overallProgress = (double) progress.getCompletedMods() / progress.getTotalMods();
                            downloadProgress.setProgress(overallProgress);
                            statusLabel.setText(String.format("Downloading modset '%s': %s (%d/%d mods completed)", 
                                modSetName, 
                                progress.getCurrentModName(), 
                                progress.getCompletedMods(), 
                                progress.getTotalMods()));
                        });
                    }
                );
                
                downloadFuture.thenAccept(result -> {
                    Platform.runLater(() -> {
                        downloadProgress.setProgress(1.0);
                        statusLabel.setText("✓ Modset download completed: " + modSetName);
                        
                        String resultMessage = String.format("Modset '%s' download completed!\n\n" +
                                "Download Results:\n" +
                                "• Successfully downloaded: %d mods\n" +
                                "• Failed downloads: %d mods\n" +
                                "• Skipped (already installed): %d mods\n\n" +
                                "The modset is now ready for use.", 
                                modSetName, result.getSuccessful(), result.getFailed(), result.getSkipped());
                        
                        if (result.getFailed() > 0) {
                            showError("Download Completed with Errors", resultMessage);
                        } else {
                            showInfo("Download Completed Successfully", resultMessage);
                        }
                    });
                }).exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        downloadProgress.setProgress(0);
                        statusLabel.setText("✗ Modset download failed: " + modSetName);
                        showError("Modset Download Failed", 
                                "Failed to download modset '" + modSetName + "'\n\n" +
                                "Error: " + throwable.getMessage() + 
                                "\n\nPlease check your connection and try again.");
                    });
                    return null;
                });
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                downloadProgress.setProgress(0);
                statusLabel.setText("✗ Failed to fetch modset details");
                showError("Modset Details Failed", 
                        "Failed to get details for modset '" + modSetName + "'\n\n" +
                        "Error: " + throwable.getMessage() + 
                        "\n\nThe modset may not exist or the server may be unavailable.");
            });
            return null;
        });
    }

    @FXML
    private void updateAllModSets() {
        showInfo("Feature Coming Soon", "Bulk download functionality will be implemented in the future.");
    }
    
    @FXML
    private void showSettings() {
        SettingsDialog dialog = new SettingsDialog(modSetList.getScene().getWindow(), config);
        dialog.show(); // SettingsDialog doesn't return Optional<ClientConfig>, it's a Stage
        // The dialog will update the config internally
    }
    
    @FXML
    private void exit() {
        javafx.application.Platform.exit();
    }
    
    @FXML
    private void testSelectedRepositoryFromStatus() {
        Repository selectedRepo = repositoryStatusTable.getSelectionModel().getSelectedItem();
        if (selectedRepo == null) {
            showError("No Repository Selected", "Please select a repository to test.");
            return;
        }

        // Create a repository service for this specific repository
        RepositoryService repositoryService = new RepositoryService(config);
        repositoryService.setRepositoryUrl(selectedRepo.getUrl());

        // Test the connection asynchronously
        statusLabel.setText("Testing connection to " + selectedRepo.getName() + "...");
        
        repositoryService.testConnectionAsync().thenAccept(healthStatus -> {
            Platform.runLater(() -> {
                switch (healthStatus) {
                    case HEALTHY:
                        statusLabel.setText("Connection to " + selectedRepo.getName() + " successful!");
                        selectedRepo.setHealthStatus(healthStatus);
                        selectedRepo.setLastChecked(java.time.LocalDateTime.now());
                        repositoryStatusTable.refresh();
                        showInfo("Connection Test", "Successfully connected to " + selectedRepo.getName());
                        break;
                    case DEGRADED:
                        statusLabel.setText("Connection to " + selectedRepo.getName() + " has issues.");
                        selectedRepo.setHealthStatus(healthStatus);
                        selectedRepo.setLastChecked(java.time.LocalDateTime.now());
                        repositoryStatusTable.refresh();
                        showError("Connection Test", "Connected to " + selectedRepo.getName() + " but service is degraded.");
                        break;
                    case ERROR:
                        statusLabel.setText("Failed to connect to " + selectedRepo.getName());
                        selectedRepo.setHealthStatus(healthStatus);
                        selectedRepo.setLastChecked(java.time.LocalDateTime.now());
                        repositoryStatusTable.refresh();
                        showError("Connection Test", "Failed to connect to " + selectedRepo.getName() + 
                                "\n\nPlease verify:\n- The server is running\n- The URL is correct\n- Your network connection");
                        break;
                }
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                statusLabel.setText("Connection test failed");
                selectedRepo.setHealthStatus(HealthStatus.ERROR);
                selectedRepo.setLastChecked(java.time.LocalDateTime.now());
                repositoryStatusTable.refresh();
                showError("Connection Test Error", "Failed to test connection: " + throwable.getMessage());
            });
            return null;
        });
    }
    

    
    private String formatSize(long bytes) {
        if (bytes <= 0) return "Unknown";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    public void navigateToRepositoryManagement() {
        // Since updates are now part of Repository Management tab,
        // we don't need to do anything special - just refresh updates
        refreshUpdates();
    }
    
    private String getRepositoryStatusText(Repository repository) {
        if (repository.getHealthStatus() == null) {
            return "Unknown";
        }
        return switch (repository.getHealthStatus()) {
            case HEALTHY -> "Online";
            case DEGRADED -> "Degraded";
            case ERROR -> "Offline";
            case UNKNOWN -> "Unknown";
        };
    }
    
    private void updateRepositoryStatusDetails(Repository repository) {
        if (repositoryDetailsArea != null && repository != null) {
            StringBuilder details = new StringBuilder();
            details.append("Repository: ").append(repository.getName()).append("\n");
            details.append("URL: ").append(repository.getUrl()).append("\n");
            details.append("Status: ").append(repository.getHealthStatus()).append("\n");
            details.append("Mod Count: ").append(repository.getModCount()).append("\n");
            details.append("Mod Set Count: ").append(repository.getModSetCount()).append("\n");
            details.append("Total Size: ").append(repository.getTotalSize()).append("\n");
            details.append("Last Checked: ").append(repository.getLastChecked()).append("\n");
            repositoryDetailsArea.setText(details.toString());
        } else if (repositoryDetailsArea != null) {
            repositoryDetailsArea.clear();
        }
    }
    
    @FXML
    private void launchGame() {
        RepositoryModSet selectedModSet = modSetList.getSelectionModel().getSelectedItem();
        ModSet modSet = selectedModSet != null ? selectedModSet.getModSet() : null;
        
        GameLaunchDialog dialog = new GameLaunchDialog(modSetList.getScene().getWindow(), config, modSet);
        dialog.showAndWait();
    }
    
    @FXML
    private void updateMods() {
        RepositoryModSet selectedModSet = modSetList.getSelectionModel().getSelectedItem();
        if (selectedModSet == null) {
            showError("No mod set selected", "Please select a mod set to update.");
            return;
        }
        
        // TODO: Implement mod update functionality
        showInfo("Update Mods", "Mod update functionality will be implemented here.");
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }



    @FXML
    private void deleteSelectedRepository() {
        Repository selectedRepo = repositoryStatusTable.getSelectionModel().getSelectedItem();
        if (selectedRepo == null) {
            showError("No Repository Selected", "Please select a repository to delete.");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete Repository");
        confirmDialog.setContentText("Are you sure you want to delete the repository '" + 
                                   selectedRepo.getName() + "'?\n\nThis will remove all connection details but will not delete downloaded mods.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Remove from repositories list
                repositories.remove(selectedRepo);

                // Remove from config
                config.removeRepository(selectedRepo.getId());
                config.saveConfig();

                // Refresh repository combo box in mod sets tab
                repositoryComboBox.getSelectionModel().clearSelection();

                showInfo("Repository Deleted", "Repository '" + selectedRepo.getName() + "' has been deleted successfully.");
            } catch (Exception e) {
                showError("Delete Error", "Failed to delete repository: " + e.getMessage());
            }
        }
    }
}
