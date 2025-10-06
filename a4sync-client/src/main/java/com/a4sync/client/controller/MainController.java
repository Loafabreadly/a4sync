package com.a4sync.client.controller;

import com.a4sync.client.config.ClientConfig;
import com.a4sync.client.model.Repository;
import com.a4sync.client.model.RepositoryModSet;
import com.a4sync.client.service.GameLauncher;
import com.a4sync.client.service.ModManager;
import com.a4sync.client.service.MultiRepositoryService;
import com.a4sync.client.service.RepositoryManager;
import com.a4sync.client.service.RepositoryManagerFactory;
import com.a4sync.client.service.RepositoryService;
import com.a4sync.common.model.GameType;
import com.a4sync.common.model.Mod;
import com.a4sync.common.model.ModSet;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;

import java.util.Optional;
import com.a4sync.client.model.HealthStatus;
import java.time.format.DateTimeFormatter;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MainController {
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
    @FXML private TableView<RepositoryManager.ModSetStatus> updatesTable;
    @FXML private TableColumn<RepositoryManager.ModSetStatus, String> updateModSetColumn;
    @FXML private TableColumn<RepositoryManager.ModSetStatus, String> updateRepositoryColumn;
    @FXML private TableColumn<RepositoryManager.ModSetStatus, String> updateCurrentVersionColumn;
    @FXML private TableColumn<RepositoryManager.ModSetStatus, String> updateNewVersionColumn;
    
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
    private final ObservableList<RepositoryManager.ModSetStatus> updateStatuses;
    
    public MainController() {
        this.config = new ClientConfig();
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
            RepositoryManager.ModSetStatus status = cellData.getValue();
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
            
            if (modSet.getGameOptions() != null) {
                profileName.setText(modSet.getGameOptions().getProfileName() != null ? 
                    modSet.getGameOptions().getProfileName() : "");
                gameTypeComboBox.setValue(modSet.getGameOptions().getGameType() != null ? 
                    modSet.getGameOptions().getGameType() : GameType.ARMA_4);
                noSplashCheck.setSelected(modSet.getGameOptions().isNoSplash());
            } else {
                profileName.clear();
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
            
            // Test connection first
            CompletableFuture.supplyAsync(() -> {
                try {
                    java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                    java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(newRepo.getUrl() + "/api/modsets"))
                        .timeout(java.time.Duration.ofSeconds(10))
                        .GET()
                        .build();
                    
                    java.net.http.HttpResponse<String> response = client.send(request, 
                        java.net.http.HttpResponse.BodyHandlers.ofString());
                    return response.statusCode() == 200;
                } catch (Exception e) {
                    return false;
                }
            }).thenAccept(connectionSuccess -> Platform.runLater(() -> {
                if (connectionSuccess) {
                    multiRepositoryService.addRepository(newRepo);
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
                        loadRepositories();
                        repositoryComboBox.getSelectionModel().select(newRepo);
                        statusLabel.setText("Repository '" + newRepo.getName() + "' added (offline)");
                    }
                }
            }));
        }
    }
    
    @FXML
    private void manageRepositories() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Repository Manager");
        info.setContentText("Advanced repository management coming soon!");
        info.showAndWait();
    }
    
        @FXML
    private void refreshAllRepositories() {
        // TODO: Implement refresh all repositories functionality
        System.out.println("Refresh all repositories action triggered");
    }

    @FXML
    private void testSelectedRepository() {
        // TODO: Implement test selected repository functionality
        System.out.println("Test selected repository action triggered");
    }

    @FXML
    private void addSearchDirectory() {
        // TODO: Implement add search directory functionality
        System.out.println("Add search directory action triggered");
    }

    @FXML
    private void createModSet() {
        // TODO: Implement create mod set functionality
        System.out.println("Create mod set action triggered");
    }

    @FXML
    private void deleteModSet() {
        // TODO: Implement delete mod set functionality
        System.out.println("Delete mod set action triggered");
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
                selectedRepo.setModSetCount(repositoryInfo.getModSetCount());
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
                                  repositoryInfo.getModSetCount() + " mod sets available for download");
                downloadProgress.setProgress(0);
                
                showInfo("Repository Connected", "Successfully connected to " + selectedRepo.getName() + 
                        "\n\nRepository: " + repositoryInfo.getName() +
                        "\nLast Updated: " + repositoryInfo.getLastUpdated().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) +
                        "\nAvailable Mod Sets: " + repositoryInfo.getModSetCount() +
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
        // TODO: Implement refresh updates functionality
        System.out.println("Refresh updates action triggered");
    }

    @FXML
    private void updateSelectedModSets() {
        // TODO: Implement update selected mod sets functionality
        System.out.println("Update selected mod sets action triggered");
    }

    @FXML
    private void updateAllModSets() {
        // TODO: Implement update all mod sets functionality
        System.out.println("Update all mod sets action triggered");
    }
    
    @FXML 
    private void showLocalModManager() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Local Mod Manager");
        info.setContentText("Local mod management coming soon!");
        info.showAndWait();
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
    private void scanLocalDirectories() {
        showLocalModManager(); // For now, redirect to local mod manager
    }
    
    @FXML
    private void cleanupOrphanedFiles() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Cleanup");
        info.setContentText("Cleanup feature coming soon!");
        info.showAndWait();
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
