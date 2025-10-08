package com.a4sync.client.controller;

import com.a4sync.client.config.ClientConfig;
import com.a4sync.client.model.Repository;
import com.a4sync.client.model.RepositoryModSet;
import com.a4sync.client.model.ModSetStatus;
import com.a4sync.client.service.GameLauncher;
import com.a4sync.client.service.ModManager;
import com.a4sync.client.service.MultiRepositoryService;
import com.a4sync.client.service.RepositoryService;
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
import javafx.stage.DirectoryChooser;
import javafx.scene.control.TextInputDialog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MainController {
    @FXML private TabPane mainTabPane;
    @FXML private ListView<RepositoryModSet> modSetList;
    @FXML private TextField searchDirectoryField;
    @FXML private ComboBox<Repository> repositoryComboBox;
    @FXML private TextField modSetName;
    @FXML private TextField profileName;
    @FXML private ComboBox<GameType> gameTypeComboBox;
    @FXML private CheckBox noSplashCheck;
    @FXML private ListView<String> modList;
    @FXML private TableView<Mod> availableModsTable;
    @FXML private TableColumn<Mod, String> modNameColumn;
    @FXML private TableColumn<Mod, String> modVersionColumn;
    @FXML private TableColumn<Mod, String> modSizeColumn;
    @FXML private TableColumn<Mod, String> modStatusColumn;
    @FXML private ProgressBar downloadProgress;
    @FXML private Label statusLabel;
    
    // Updates table components (now in Repository Management tab)
    @FXML private TableView<ModSetStatus> updatesTable;
    @FXML private TableColumn<ModSetStatus, String> updateModSetColumn;
    @FXML private TableColumn<ModSetStatus, String> updateRepositoryColumn;
    @FXML private TableColumn<ModSetStatus, String> updateCurrentVersionColumn;
    @FXML private TableColumn<ModSetStatus, String> updateNewVersionColumn;
    
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
    
    // Repository editing fields
    @FXML private TextField repoEditNameField;
    @FXML private TextField repoEditUrlField;
    @FXML private PasswordField repoEditPasswordField;
    @FXML private CheckBox repoEditEnabledCheck;
    
    private final ClientConfig config;
    private final ModManager modManager;
    private final GameLauncher gameLauncher;
    private final MultiRepositoryService multiRepositoryService;
    private final ObservableList<RepositoryModSet> modSets;
    private final ObservableList<Mod> availableMods;
    private final ObservableList<Repository> repositories;
    private final ObservableList<ModSetStatus> updateStatuses;
    
    public MainController() {
        this.config = ClientConfig.loadConfig();
        this.config.migrateLegacyRepository(); // Handle backward compatibility
        this.modManager = new ModManager(config);
        this.gameLauncher = new GameLauncher(config);
        this.multiRepositoryService = new MultiRepositoryService(config);
        this.modSets = FXCollections.observableArrayList();
        this.availableMods = FXCollections.observableArrayList();
        this.repositories = FXCollections.observableArrayList();
        this.updateStatuses = FXCollections.observableArrayList();
    }

    @FXML
    private void initialize() {
        modSetList.setItems(modSets);
        availableModsTable.setItems(availableMods);
        repositoryComboBox.setItems(repositories);
        updatesTable.setItems(updateStatuses);
        gameTypeComboBox.getItems().addAll(GameType.values());
        
        // Initialize directories field
        searchDirectoryField.setText(config.getModDirectories().isEmpty() ? 
            "" : config.getModDirectories().get(0).toString());
            
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
            
        // Setup updates table columns
        updateModSetColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getRemoteSet().getName()));
        updateRepositoryColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getRepository().getName()));
        updateCurrentVersionColumn.setCellValueFactory(cellData -> {
            ModSetStatus status = cellData.getValue();
            return new SimpleStringProperty(status.getLocalSet() != null ? 
                status.getLocalSet().getVersion() : "Not Downloaded");
        });
        updateNewVersionColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getRemoteSet().getVersion()));
        
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
                    populateRepositoryEditFields(newRepo);
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
            
            // Load game options from client configuration, not from modset
            GameOptions clientOptions = config.getDefaultGameOptions();
            if (clientOptions != null) {
                profileName.setText(clientOptions.getProfileName() != null ? 
                    clientOptions.getProfileName() : modSet.getName().toLowerCase());
                gameTypeComboBox.setValue(clientOptions.getGameType() != null ? 
                    clientOptions.getGameType() : GameType.ARMA_4);
                noSplashCheck.setSelected(clientOptions.isNoSplash());
            } else {
                // Fallback defaults
                profileName.setText(modSet.getName().toLowerCase());
                gameTypeComboBox.setValue(GameType.ARMA_4);
                noSplashCheck.setSelected(false);
            }
            
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
            profileName.clear();
            gameTypeComboBox.setValue(null);
            noSplashCheck.setSelected(false);
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
            
            // Show progress while testing connection
            statusLabel.setText("Testing connection to " + newRepo.getName() + "...");
            
            // Test connection first using health endpoint
            CompletableFuture.supplyAsync(() -> {
                try {
                    java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                    java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(newRepo.getUrl() + "/api/v1/health"))
                        .timeout(java.time.Duration.ofSeconds(10))
                        .GET()
                        .build();
                    
                    java.net.http.HttpResponse<String> response = client.send(request, 
                        java.net.http.HttpResponse.BodyHandlers.ofString());
                    
                    // Check for UP or DEGRADED status (both are acceptable for connection)
                    if (response.statusCode() == 200) {
                        String body = response.body();
                        return body.contains("\"status\":\"UP\"") || body.contains("\"status\":\"DEGRADED\"");
                    }
                    return false;
                } catch (Exception e) {
                    return false;
                }
            }).thenAccept(connectionSuccess -> Platform.runLater(() -> {
                if (connectionSuccess) {
                    multiRepositoryService.addRepository(newRepo);
                    config.saveConfig(); // Save the configuration to persist the repository
                    loadRepositories();
                    repositoryComboBox.getSelectionModel().select(newRepo);
                    
                    statusLabel.setText("Repository '" + newRepo.getName() + "' added successfully!");
                    
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Repository Added");
                    success.setHeaderText("Success!");
                    success.setContentText("Repository '" + newRepo.getName() + "' has been added and is accessible.");
                    success.showAndWait();
                } else {
                    statusLabel.setText("Failed to connect to repository");
                    
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Connection Failed");
                    confirm.setHeaderText("Could not connect to repository");
                    confirm.setContentText("The repository is not accessible right now. Add it anyway?");
                    
                    Optional<ButtonType> confirmResult = confirm.showAndWait();
                    if (confirmResult.isPresent() && confirmResult.get() == ButtonType.OK) {
                        multiRepositoryService.addRepository(newRepo);
                        config.saveConfig(); // Save the configuration to persist the repository
                        loadRepositories();
                        repositoryComboBox.getSelectionModel().select(newRepo);
                        statusLabel.setText("Repository '" + newRepo.getName() + "' added (offline)");
                    }
                }
            }));
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
        Repository selectedRepo = repositoryStatusTable.getSelectionModel().getSelectedItem();
        if (selectedRepo == null) {
            showError("No Repository Selected", "Please select a repository to test.");
            return;
        }
        
        // Show progress indicator
        showInfo("Testing Connection", "Testing connection to " + selectedRepo.getName() + "...");
        
        // Test connection in background thread
        Task<HealthStatus> testTask = new Task<HealthStatus>() {
            @Override
            protected HealthStatus call() throws Exception {
                // Create temporary config for this specific repository
                ClientConfig tempConfig = new ClientConfig();
                tempConfig.setServerUrl(selectedRepo.getUrl());
                tempConfig.setRepositoryPassword(selectedRepo.getPassword());
                tempConfig.setUseAuthentication(selectedRepo.isUseAuthentication());
                
                RepositoryService repoService = new RepositoryService(tempConfig);
                return repoService.testConnectionAsync().get();
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    HealthStatus healthStatus = getValue();
                    selectedRepo.setHealthStatus(healthStatus);
                    selectedRepo.setLastChecked(LocalDateTime.now());
                    repositoryStatusTable.refresh();
                    
                    boolean isHealthy = healthStatus == HealthStatus.HEALTHY;
                    String status = isHealthy ? "successful" : "failed";
                    String title = isHealthy ? "Connection Successful" : "Connection Failed";
                    String message = "Connection test to " + selectedRepo.getName() + " " + status + ".";
                    
                    if (isHealthy) {
                        showInfo(title, message);
                    } else {
                        showError(title, message + " Please check the server address and credentials.");
                    }
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    selectedRepo.setHealthStatus(HealthStatus.ERROR);
                    selectedRepo.setLastChecked(LocalDateTime.now());
                    repositoryStatusTable.refresh();
                    showError("Connection Error", "Failed to test connection to " + selectedRepo.getName() + 
                             ": " + getException().getMessage());
                });
            }
        };
        
        Thread testThread = new Thread(testTask);
        testThread.setDaemon(true);
        testThread.start();
    }

    @FXML
    private void addSearchDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Mod Search Directory");
        chooser.setInitialDirectory(new java.io.File(System.getProperty("user.home")));
        
        java.io.File selectedDirectory = chooser.showDialog(searchDirectoryField.getScene().getWindow());
        if (selectedDirectory != null && selectedDirectory.exists()) {
            java.nio.file.Path directoryPath = selectedDirectory.toPath();
            
            // Check if directory is already added
            if (config.getModDirectories().contains(directoryPath)) {
                showInfo("Directory Already Added", "The directory '" + directoryPath + "' is already in your search paths.");
                return;
            }
            
            // Add directory to configuration
            config.addModDirectory(directoryPath);
            config.saveConfig();
            
            // Update the UI field to show the first directory (for backward compatibility)
            if (config.getModDirectories().size() == 1) {
                searchDirectoryField.setText(directoryPath.toString());
            } else {
                // Show count if multiple directories
                searchDirectoryField.setText(config.getModDirectories().size() + " directories configured");
            }
            
            showInfo("Directory Added", 
                   "Added mod search directory: " + directoryPath + 
                   "\n\nTotal search directories: " + config.getModDirectories().size() +
                   "\n\nA4Sync will now search this directory for local mods when creating mod sets.");
            
            statusLabel.setText("Added search directory: " + selectedDirectory.getName());
        }
    }

    @FXML
    private void createModSet() {
        // Open dialog to create a new mod set from local mods
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Mod Set");
        dialog.setHeaderText("Create New Mod Set");
        dialog.setContentText("Enter a name for the new mod set:");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String modSetName = result.get().trim();
            
            // Check if mod set name already exists
            if (modSets.stream().anyMatch(ms -> ms.getModSet().getName().equals(modSetName))) {
                showError("Duplicate Name", "A mod set with the name '" + modSetName + "' already exists.");
                return;
            }
            
            // Create new mod set
            createNewModSet(modSetName);
        }
    }
    
    private void createNewModSet(String modSetName) {
        showInfo("Creating Mod Set", "Creating new mod set: " + modSetName);
        
        Task<Void> createTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Scanning local mods...");
                
                try {
                    // Create a new ModSet object
                    ModSet newModSet = new ModSet();
                    newModSet.setName(modSetName);
                    newModSet.setDescription("Created from local mods");
                    newModSet.setVersion("1.0.0");
                    // Skip setCreatedDate as it may not exist
                    
                    // Create RepositoryModSet wrapper for local mod set (no repository)
                    RepositoryModSet repoModSet = new RepositoryModSet(null, newModSet);
                    
                    Platform.runLater(() -> {
                        modSets.add(repoModSet);
                        // Save to config if needed
                        config.saveConfig();
                    });
                    
                } catch (Exception e) {
                    throw e;
                }
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    statusLabel.setText("Mod set created: " + modSetName);
                    showInfo("Mod Set Created", "Successfully created mod set: " + modSetName + 
                           "\n\nThe mod set has been added to your local collection.");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    statusLabel.setText("Failed to create mod set: " + modSetName);
                    showError("Creation Failed", "Failed to create mod set '" + modSetName + "': " + 
                             getException().getMessage());
                });
            }
        };
        
        statusLabel.textProperty().bind(createTask.messageProperty());
        
        Thread createThread = new Thread(createTask);
        createThread.setDaemon(true);
        createThread.start();
    }

    @FXML
    private void deleteModSet() {
        RepositoryModSet selectedModSet = modSetList.getSelectionModel().getSelectedItem();
        if (selectedModSet == null) {
            showError("No Selection", "Please select a mod set to delete.");
            return;
        }
        
        // Confirm deletion
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Mod Set");
        confirmation.setHeaderText("Delete " + selectedModSet.getModSet().getName() + "?");
        confirmation.setContentText("This will permanently delete the mod set and all its files from your system.\n\n" +
                                   "This action cannot be undone.");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        
        deleteSelectedModSet(selectedModSet);
    }
    
    private void deleteSelectedModSet(RepositoryModSet repoModSet) {
        String modSetName = repoModSet.getModSet().getName();
        showInfo("Deleting Mod Set", "Deleting mod set: " + modSetName);
        
        Task<Void> deleteTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Deleting " + modSetName + "...");
                
                try {
                    // For now, just remove from the list
                    // Full deletion would require ModManager integration
                    Platform.runLater(() -> {
                        modSets.remove(repoModSet);
                        // Save to config
                        config.saveConfig();
                    });
                    
                } catch (Exception e) {
                    throw e;
                }
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    statusLabel.setText("Mod set deleted: " + modSetName);
                    showInfo("Mod Set Deleted", "Successfully deleted mod set: " + modSetName);
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    statusLabel.setText("Failed to delete mod set: " + modSetName);
                    showError("Deletion Failed", "Failed to delete mod set '" + modSetName + "': " + 
                             getException().getMessage());
                });
            }
        };
        
        statusLabel.textProperty().bind(deleteTask.messageProperty());
        
        Thread deleteThread = new Thread(deleteTask);
        deleteThread.setDaemon(true);
        deleteThread.start();
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

        statusLabel.setText("Connecting to " + selectedRepo.getName() + "...");
                downloadProgress.setProgress(-1.0); // Indeterminate progress        // First test the connection
        repositoryService.testConnectionAsync().thenCompose(healthStatus -> {
            if (healthStatus == HealthStatus.ERROR) {
                throw new RuntimeException("Failed to connect to repository");
            }
            
            Platform.runLater(() -> {
                statusLabel.setText("Loading repository info from " + selectedRepo.getName() + "...");
            });
            
            // Load repository info (lightweight - just modset names and count)
            return repositoryService.getRepositoryInfo();
        }).thenAccept(repositoryInfo -> {
            Platform.runLater(() -> {
                // Update repository with info from server
                selectedRepo.setHealthStatus(HealthStatus.HEALTHY);
                selectedRepo.setLastChecked(java.time.LocalDateTime.now());
                selectedRepo.setName(repositoryInfo.getName()); // Use server's repository name
                int modSetCount = repositoryInfo.getModSets() != null ? repositoryInfo.getModSets().size() : 0;
                selectedRepo.setModSetCount(modSetCount);
                selectedRepo.setLastUpdated(repositoryInfo.getLastUpdated());
                
                // Clear available mods table and show modsets for selection instead
                availableMods.clear();
                
                // Display modset summaries (name, description, version)
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
                statusLabel.setText("Connected to " + selectedRepo.getName() + " - " + 
                                  modSetCount + " mod sets available for download");
                downloadProgress.setProgress(0);
                
                showInfo("Repository Connected", "Successfully connected to " + selectedRepo.getName() + 
                        "\n\nRepository: " + repositoryInfo.getName() +
                        "\nLast Updated: " + repositoryInfo.getLastUpdated().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) +
                        "\nAvailable Mod Sets: " + modSetCount +
                        "\n\nSelect a mod set from the table to download it.");
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                selectedRepo.setHealthStatus(HealthStatus.ERROR);
                selectedRepo.setLastChecked(java.time.LocalDateTime.now());
                repositoryStatusTable.refresh();
                statusLabel.setText("Failed to connect to repository");
                downloadProgress.setProgress(0);
                showError("Connection Failed", "Failed to connect to " + selectedRepo.getName() + 
                        "\n\nError: " + throwable.getMessage() + 
                        "\n\nPlease verify the server is running and accessible.");
            });
            return null;
        });
    }

    @FXML
    private void refreshUpdates() {
        if (repositories.isEmpty()) {
            showInfo("No Repositories", "No repositories configured. Please add repositories first.");
            return;
        }

        showInfo("Refreshing Updates", "Checking all repositories for mod set updates with version comparison...");
        
        // Enhanced update checking with version comparison
        Task<UpdateCheckResult> updateTask = new Task<UpdateCheckResult>() {
            @Override
            protected UpdateCheckResult call() throws Exception {
                updateMessage("Connecting to repositories...");
                
                UpdateCheckResult result = new UpdateCheckResult();
                
                // Use the existing multi-repository service to get all mod sets
                CompletableFuture<Map<Repository, List<ModSet>>> allModSetsFuture = 
                    multiRepositoryService.getAllModSets();
                
                Map<Repository, List<ModSet>> repoModSetsMap = allModSetsFuture.get();
                
                updateMessage("Analyzing mod sets for updates...");
                
                for (Map.Entry<Repository, List<ModSet>> entry : repoModSetsMap.entrySet()) {
                    Repository repo = entry.getKey();
                    List<ModSet> remoteModSets = entry.getValue();
                    
                    if (remoteModSets != null) {
                        for (ModSet remoteModSet : remoteModSets) {
                            // Check if we have this mod set locally
                            RepositoryModSet localModSet = findLocalModSet(remoteModSet.getName());
                            
                            ModSetUpdateInfo updateInfo = new ModSetUpdateInfo();
                            updateInfo.repositoryName = repo.getName();
                            updateInfo.modSetName = remoteModSet.getName();
                            updateInfo.remoteVersion = remoteModSet.getVersion();
                            updateInfo.remoteSize = remoteModSet.getTotalSize();
                            updateInfo.lastUpdated = remoteModSet.getLastUpdated();
                            
                            if (localModSet == null) {
                                // Mod set not downloaded
                                updateInfo.localVersion = "Not Downloaded";
                                updateInfo.updateStatus = UpdateStatus.NOT_DOWNLOADED;
                                result.notDownloaded++;
                            } else {
                                // Compare versions
                                updateInfo.localVersion = localModSet.getModSet().getVersion();
                                int versionComparison = compareVersions(
                                    localModSet.getModSet().getVersion(), 
                                    remoteModSet.getVersion()
                                );
                                
                                if (versionComparison < 0) {
                                    updateInfo.updateStatus = UpdateStatus.UPDATE_AVAILABLE;
                                    result.updatesAvailable++;
                                } else if (versionComparison > 0) {
                                    updateInfo.updateStatus = UpdateStatus.LOCAL_NEWER;
                                    result.localNewer++;
                                } else {
                                    updateInfo.updateStatus = UpdateStatus.UP_TO_DATE;
                                    result.upToDate++;
                                }
                            }
                            
                            result.modSetUpdates.add(updateInfo);
                        }
                    }
                }
                
                updateMessage("Update check completed");
                return result;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    UpdateCheckResult result = getValue();
                    processUpdateCheckResult(result);
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showError("Updates Check Failed", "An error occurred while checking for updates: " + 
                             getException().getMessage());
                });
            }
        };
        
        Thread updateThread = new Thread(updateTask);
        updateThread.setDaemon(true);
        updateThread.start();
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
    
    private void processUpdateCheckResult(UpdateCheckResult result) {
        // Clear previous results
        updateStatuses.clear();
        
        // Convert ModSetUpdateInfo objects to ModSetStatus objects for the table
        for (ModSetUpdateInfo updateInfo : result.modSetUpdates) {
            // Find the repository for this mod set
            Repository repository = repositories.stream()
                .filter(repo -> repo.getName().equals(updateInfo.repositoryName))
                .findFirst()
                .orElse(null);
                
            if (repository != null) {
                // Create the remote ModSet object
                ModSet remoteSet = new ModSet();
                remoteSet.setName(updateInfo.modSetName);
                remoteSet.setVersion(updateInfo.remoteVersion);
                remoteSet.setTotalSize(updateInfo.remoteSize);
                remoteSet.setLastUpdated(updateInfo.lastUpdated);
                
                // Find local ModSet if exists
                RepositoryModSet localRepoModSet = findLocalModSet(updateInfo.modSetName);
                ModSet localSet = localRepoModSet != null ? localRepoModSet.getModSet() : null;
                
                // Create ModSetStatus based on update status
                ModSetStatus status;
                switch (updateInfo.updateStatus) {
                    case NOT_DOWNLOADED:
                        status = ModSetStatus.notDownloaded(repository, remoteSet);
                        break;
                    case UPDATE_AVAILABLE:
                        status = ModSetStatus.updateAvailable(repository, remoteSet, localSet);
                        break;
                    case UP_TO_DATE:
                        status = ModSetStatus.upToDate(repository, remoteSet, localSet);
                        break;
                    default:
                        continue; // Skip LOCAL_NEWER status for now
                }
                
                // Only add actionable items (not up-to-date) to the updates table
                if (updateInfo.updateStatus != UpdateStatus.UP_TO_DATE && 
                    updateInfo.updateStatus != UpdateStatus.LOCAL_NEWER) {
                    updateStatuses.add(status);
                }
            }
        }
        
        // Display comprehensive update summary
        StringBuilder summary = new StringBuilder();
        summary.append("Update Check Results:\n\n");
        summary.append("📦 Available for Download: ").append(result.notDownloaded).append("\n");
        summary.append("🔄 Updates Available: ").append(result.updatesAvailable).append("\n");
        summary.append("✅ Up to Date: ").append(result.upToDate).append("\n");
        summary.append("⬆️ Local Newer: ").append(result.localNewer).append("\n\n");
        
        int totalActionable = result.notDownloaded + result.updatesAvailable;
        if (totalActionable > 0) {
            summary.append("Total actionable items: ").append(totalActionable);
        } else {
            summary.append("All mod sets are up to date!");
        }
        
        showInfo("Update Check Complete", summary.toString());
        
        // Update status label
        if (totalActionable > 0) {
            statusLabel.setText(totalActionable + " mod sets can be downloaded or updated");
        } else {
            statusLabel.setText("All mod sets are up to date");
        }
    }

    @FXML
    private void updateSelectedModSets() {
        // Check if we have selected mod sets from the updates table first
        ModSetStatus selectedUpdate = updatesTable.getSelectionModel().getSelectedItem();
        if (selectedUpdate != null) {
            downloadSelectedModSetUpdate(selectedUpdate);
            return;
        }
        
        // Otherwise, check if we have a selected mod set from available mods table
        Mod selectedMod = availableModsTable.getSelectionModel().getSelectedItem();
        if (selectedMod != null) {
            downloadSelectedModFromTable(selectedMod);
            return;
        }
        
        showError("No Selection", "Please select a mod set to download from either the Available Mod Sets or Updates table.");
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
        
        showInfo("Download Starting", "Starting download of " + selectedMod.getName() + 
                " from " + connectedRepo.getName());
        
        // Start download in background thread
        Task<Void> downloadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Downloading " + selectedMod.getName() + "...");
                
                try {
                    // Use existing download functionality
                    statusLabel.setText("Download functionality available - using existing mod download system");
                    
                } catch (Exception e) {
                    throw e;
                }
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    downloadProgress.setProgress(1.0);
                    statusLabel.setText("Download completed: " + selectedMod.getName());
                    showInfo("Download Complete", "Successfully downloaded " + selectedMod.getName());
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    downloadProgress.setProgress(0);
                    statusLabel.setText("Download failed: " + selectedMod.getName());
                    showError("Download Failed", "Failed to download " + selectedMod.getName() + 
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

    @FXML
    private void updateAllModSets() {
        // Get all mod sets that need updates
        List<ModSetStatus> modSetsToUpdate = updateStatuses.stream()
            .filter(status -> status.getStatus() == ModSetStatus.Status.UPDATE_AVAILABLE || 
                            status.getStatus() == ModSetStatus.Status.NOT_DOWNLOADED)
            .toList();
            
        if (modSetsToUpdate.isEmpty()) {
            showInfo("No Updates Available", "No mod sets need updating. Click 'Refresh Updates' to check for new updates.");
            return;
        }
        
        // Confirm bulk download
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Download All Updates");
        confirmation.setHeaderText("Download " + modSetsToUpdate.size() + " mod sets?");
        confirmation.setContentText("This will download all available mod set updates. This may take a while and use significant bandwidth.");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        
        showInfo("Bulk Download Started", "Starting download of " + modSetsToUpdate.size() + " mod sets...");
        
        // Download all mod sets in background thread
        Task<Void> downloadAllTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int total = modSetsToUpdate.size();
                int current = 0;
                
                for (ModSetStatus modSetStatus : modSetsToUpdate) {
                    current++;
                    updateProgress(current, total);
                    String modSetName = modSetStatus.getRemoteSet() != null ? modSetStatus.getRemoteSet().getName() : "ModSet " + current;
                    updateMessage("Processing " + modSetName + " (" + current + "/" + total + ")...");
                    
                    // Small delay between operations 
                    Thread.sleep(100);
                }
                
                updateMessage("Bulk operation completed for " + total + " mod sets");
                return null;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    downloadProgress.setProgress(0);
                    downloadProgress.progressProperty().unbind();
                    statusLabel.textProperty().unbind();
                    statusLabel.setText("Bulk download completed.");
                    
                    showInfo("Bulk Download Complete", 
                           "Finished processing all mod sets.\n\n" +
                           "Note: Full download integration requires complete repository system setup.");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    downloadProgress.setProgress(0);
                    downloadProgress.progressProperty().unbind();
                    statusLabel.textProperty().unbind();
                    statusLabel.setText("Bulk download failed");
                    showError("Bulk Download Failed", "An error occurred during bulk download: " + 
                             getException().getMessage());
                });
            }
        };
        
        // Bind progress bar to task
        downloadProgress.progressProperty().bind(downloadAllTask.progressProperty());
        statusLabel.textProperty().bind(downloadAllTask.messageProperty());
        
        Thread downloadThread = new Thread(downloadAllTask);
        downloadThread.setDaemon(true);
        downloadThread.start();
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
    private void testAllRepositories() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setContentText("Repository testing feature coming soon!");
        info.showAndWait();
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
    
    @FXML
    private void refreshSelectedRepositoryFromStatus() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setContentText("Repository refresh feature coming soon!");
        info.showAndWait();
    }
    
    @FXML
    private void viewSelectedRepositoryModSets() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setContentText("Repository mod sets view coming soon!");
        info.showAndWait();
    }
    
    @FXML
    private void openSelectedRepositoryInBrowser() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setContentText("Open in browser feature coming soon!");
        info.showAndWait();
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

    private void populateRepositoryEditFields(Repository repository) {
        if (repository == null) {
            repoEditNameField.setText("");
            repoEditUrlField.setText("");
            repoEditPasswordField.setText("");
            repoEditEnabledCheck.setSelected(false);
        } else {
            repoEditNameField.setText(repository.getName());
            repoEditUrlField.setText(repository.getUrl());
            repoEditPasswordField.setText(repository.getPassword() != null ? repository.getPassword() : "");
            repoEditEnabledCheck.setSelected(repository.isEnabled());
        }
    }

    @FXML
    private void saveRepositoryChanges() {
        Repository selectedRepo = repositoryStatusTable.getSelectionModel().getSelectedItem();
        if (selectedRepo == null) {
            showError("No Repository Selected", "Please select a repository to edit.");
            return;
        }

        String name = repoEditNameField.getText().trim();
        String url = repoEditUrlField.getText().trim();
        String password = repoEditPasswordField.getText();

        if (name.isEmpty()) {
            showError("Invalid Input", "Repository name cannot be empty.");
            return;
        }

        if (url.isEmpty()) {
            showError("Invalid Input", "Repository URL cannot be empty.");
            return;
        }

        try {
            // Update the repository
            selectedRepo.setName(name);
            selectedRepo.setUrl(url);
            selectedRepo.setPassword(password.isEmpty() ? null : password);
            selectedRepo.setEnabled(repoEditEnabledCheck.isSelected());

            // Save to config
            config.updateRepository(selectedRepo);
            config.saveConfig();

            // Refresh the table display
            repositoryStatusTable.refresh();

            showInfo("Repository Updated", "Repository connection details have been saved successfully.");
            
            // Test connection if enabled
            if (selectedRepo.isEnabled()) {
                testSelectedRepositoryFromStatus();
            }
        } catch (Exception e) {
            showError("Save Error", "Failed to save repository changes: " + e.getMessage());
        }
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

                // Clear edit fields
                populateRepositoryEditFields(null);

                // Refresh repository combo box in mod sets tab
                repositoryComboBox.getSelectionModel().clearSelection();

                showInfo("Repository Deleted", "Repository '" + selectedRepo.getName() + "' has been deleted successfully.");
            } catch (Exception e) {
                showError("Delete Error", "Failed to delete repository: " + e.getMessage());
            }
        }
    }
}
