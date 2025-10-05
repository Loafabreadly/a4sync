package com.a4sync.client.controller;

import com.a4sync.client.config.ClientConfig;
import com.a4sync.client.model.Repository;
import com.a4sync.client.model.RepositoryModSet;
import com.a4sync.client.service.GameLauncher;
import com.a4sync.client.service.ModManager;
import com.a4sync.client.service.MultiRepositoryService;
import com.a4sync.client.service.RepositoryManager;
import com.a4sync.client.service.RepositoryManagerFactory;
import com.a4sync.common.model.Mod;
import com.a4sync.common.model.ModSet;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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
    @FXML private CheckBox noSplashCheck;
    @FXML private ListView<String> modList;
    @FXML private TableView<Mod> availableModsTable;
    @FXML private TableColumn<Mod, String> modNameColumn;
    @FXML private TableColumn<Mod, String> modVersionColumn;
    @FXML private TableColumn<Mod, String> modSizeColumn;
    @FXML private TableColumn<Mod, String> modStatusColumn;
    @FXML private ProgressBar downloadProgress;
    @FXML private Label statusLabel;
    
    // Updates tab components
    @FXML private Tab updatesTab;
    @FXML private TableView<RepositoryManager.ModSetStatus> updatesTable;
    @FXML private TableColumn<RepositoryManager.ModSetStatus, String> updateModSetColumn;
    @FXML private TableColumn<RepositoryManager.ModSetStatus, String> updateRepositoryColumn;
    @FXML private TableColumn<RepositoryManager.ModSetStatus, String> updateCurrentVersionColumn;
    @FXML private TableColumn<RepositoryManager.ModSetStatus, String> updateNewVersionColumn;
    @FXML private TableColumn<RepositoryManager.ModSetStatus, String> updateActionColumn;
    @FXML private ProgressBar updateProgress;
    @FXML private Label updateStatusLabel;
    
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
        
        // Initialize directories field
        searchDirectoryField.setText(config.getModDirectories().isEmpty() ? 
            "" : config.getModDirectories().get(0).toString());
            
        // Setup table columns
        modNameColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getName()));
        modVersionColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getVersion()));
        modSizeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(formatSize(cellData.getValue().getSize())));
        modStatusColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(modManager.isModInstalled(cellData.getValue()) ? "Installed" : "Not Installed"));
            
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
        updateActionColumn.setCellValueFactory(cellData -> {
            RepositoryManager.ModSetStatus status = cellData.getValue();
            return new SimpleStringProperty(
                status.getStatus() == RepositoryManager.ModSetStatus.Status.NOT_DOWNLOADED ? "Download" : "Update"
            );
        });
            
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
            profileName.setText(modSet.getGameOptions().getProfileName());
            noSplashCheck.setSelected(modSet.getGameOptions().isNoSplash());
            
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
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/repository.fxml"));
            javafx.scene.Parent root = loader.load();
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Repository Management");
            stage.setScene(new javafx.scene.Scene(root, 800, 600));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.show();
            
            // Refresh repositories when window closes
            stage.setOnHidden(e -> loadRepositories());
            
        } catch (Exception e) {
            showError("Failed to open Repository Management", 
                "Could not open the repository management window: " + e.getMessage());
        }
    }
    
    private void removeRepository() {
        if (repositories.isEmpty()) {
            showInfo("No Repositories", "No repositories available to remove.");
            return;
        }
        
        ChoiceDialog<Repository> dialog = new ChoiceDialog<>(repositories.get(0), repositories);
        dialog.setTitle("Remove Repository");
        dialog.setHeaderText("Select repository to remove:");
        dialog.setContentText("Repository:");
        
        Optional<Repository> result = dialog.showAndWait();
        if (result.isPresent()) {
            Repository toRemove = result.get();
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Confirm Removal");
            confirmDialog.setHeaderText("Are you sure you want to remove this repository?");
            confirmDialog.setContentText("Repository: " + toRemove.getDisplayName());
            
            Optional<ButtonType> confirmation = confirmDialog.showAndWait();
            if (confirmation.isPresent() && confirmation.get() == ButtonType.OK) {
                config.removeRepository(toRemove.getId());
                loadRepositories();
                showInfo("Repository Removed", "Repository '" + toRemove.getDisplayName() + "' has been removed.");
            }
        }
    }
    
    @FXML
    private void refreshAllRepositories() {
        statusLabel.setText("Refreshing all repositories...");
        multiRepositoryService.getAllModSets()
            .thenAccept(this::updateModSetsFromRepositories)
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    showError("Refresh Failed", "Failed to refresh repositories: " + throwable.getMessage());
                    statusLabel.setText("Failed to refresh repositories");
                });
                return null;
            });
    }
    
    @FXML
    private void testSelectedRepository() {
        Repository selected = repositoryComboBox.getSelectionModel().getSelectedItem();
        if (selected != null) {
            statusLabel.setText("Testing connection to " + selected.getDisplayName() + "...");
            multiRepositoryService.testConnection(selected)
                .thenRun(() -> Platform.runLater(() -> {
                    statusLabel.setText("Connection successful");
                    showInfo("Connection Test", "Successfully connected to " + selected.getDisplayName());
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Connection failed");
                        showError("Connection Failed", "Failed to connect to " + selected.getDisplayName() + ": " + throwable.getMessage());
                    });
                    return null;
                });
        } else {
            showError("No Repository Selected", "Please select a repository to test");
        }
    }
    
    private void updateModSetsFromRepositories(Map<Repository, List<ModSet>> repositoryModSets) {
        Platform.runLater(() -> {
            modSets.clear();
            repositoryModSets.forEach((repo, modSetsList) -> {
                modSetsList.forEach(modSet -> {
                    modSets.add(new RepositoryModSet(repo, modSet));
                });
            });
            statusLabel.setText("Loaded " + modSets.size() + " mod sets from " + repositoryModSets.size() + " repositories");
        });
    }
    
    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    @FXML
    private void showSettings() {
        SettingsDialog dialog = new SettingsDialog(
            modSetList.getScene().getWindow(), 
            config
        );
        dialog.show();
    }

    @FXML
    private void exit() {
        Platform.exit();
    }

    @FXML
    private void deleteModSet() {
        RepositoryModSet selected = modSetList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Mod Set");
            alert.setHeaderText("Delete " + selected.getModSet().getName());
            alert.setContentText("Are you sure you want to delete this mod set?");
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                modSets.remove(selected);
            }
        }
    }

    @FXML
    private void refreshRepository() {
        // This method is now handled by refreshAllRepositories()
        refreshAllRepositories();
    }

    @FXML
    private void updateMods() {
        RepositoryModSet selected = modSetList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a mod set to update");
            return;
        }

        Repository repository = selected.getRepository();
        var repositoryService = multiRepositoryService.getRepositoryService(repository.getId());
        if (repositoryService == null) {
            showError("No Repository Service", "Repository service not found for " + repository.getDisplayName());
            return;
        }

        downloadProgress.setProgress(0);
        statusLabel.setText("Updating mods...");

        // Count total mods for progress
        ModSet modSet = selected.getModSet();
        int totalMods = modSet.getMods().size();
        final int[] completedMods = {0};

        modSet.getMods().forEach(mod -> {
            if (!modManager.isModInstalled(mod)) {
                modManager.downloadMod(mod, modSet.getName(), repository.getUrl())
                    .thenRun(() -> {
                        completedMods[0]++;
                        Platform.runLater(() -> {
                            downloadProgress.setProgress(
                                (double) completedMods[0] / totalMods);
                            if (completedMods[0] == totalMods) {
                                statusLabel.setText("All mods updated successfully");
                            }
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> 
                            showError("Download Failed", 
                                "Failed to download " + mod.getName() + ": " + e.getMessage()));
                        return null;
                    });
            } else {
                completedMods[0]++;
                Platform.runLater(() -> 
                    downloadProgress.setProgress((double) completedMods[0] / totalMods));
            }
        });
    }

    @FXML
    private void addSearchDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Mod Directory");
        
        var window = searchDirectoryField.getScene().getWindow();
        var directory = chooser.showDialog(window);
        
        if (directory != null) {
            config.addModDirectory(directory.toPath());
            searchDirectoryField.setText(directory.getAbsolutePath());
        }
    }

    @FXML
    private void createModSet() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Mod Set");
        dialog.setHeaderText("Enter name for new mod set");
        dialog.setContentText("Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            Repository selectedRepository = null;
            
            // Allow user to select repository for the mod set
            if (!repositories.isEmpty()) {
                ChoiceDialog<Repository> repoDialog = new ChoiceDialog<>(null, repositories);
                repoDialog.setTitle("Select Repository");
                repoDialog.setHeaderText("Choose repository for mod set (or cancel for local):");
                repoDialog.setContentText("Repository:");
                
                Optional<Repository> repoResult = repoDialog.showAndWait();
                if (repoResult.isPresent()) {
                    selectedRepository = repoResult.get();
                }
            }
            
            ModSet modSet = new ModSet();
            modSet.setName(name);
            
            // Create with selected repository (null means local mod set)
            RepositoryModSet repositoryModSet = new RepositoryModSet(selectedRepository, modSet);
            modSets.add(repositoryModSet);
            
            String message = selectedRepository != null ? 
                "Mod set '" + name + "' created for repository: " + selectedRepository.getDisplayName() :
                "Local mod set '" + name + "' created";
            showInfo("Mod Set Created", message);
        });
    }

    @FXML
    private void launchGame() {
        RepositoryModSet selected = modSetList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No mod set selected", "Please select a mod set to launch the game with.");
            return;
        }

        try {
            gameLauncher.launchGame(selected.getModSet());
        } catch (Exception e) {
            showError("Launch Failed", "Failed to launch game: " + e.getMessage());
        }
    }

    @FXML
    private void connectToRepository() {
        // This method is now handled by addRepository()
        addRepository();
    }
    
    // Updates tab methods
    @FXML
    private void refreshUpdates() {
        updateStatusLabel.setText("Checking for updates...");
        updateProgress.setProgress(-1); // Indeterminate progress
        
        RepositoryManagerFactory.getInstance().checkRepositories()
            .thenAcceptAsync(statuses -> Platform.runLater(() -> {
                updateStatuses.clear();
                // Filter to only show updates and not downloaded items
                updateStatuses.addAll(statuses.stream()
                    .filter(status -> status.getStatus() != RepositoryManager.ModSetStatus.Status.UP_TO_DATE)
                    .toList());
                updateStatusLabel.setText("Found " + updateStatuses.size() + " updates/new mod sets");
                updateProgress.setProgress(0);
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    updateStatusLabel.setText("Failed to check for updates: " + e.getMessage());
                    updateProgress.setProgress(0);
                });
                return null;
            });
    }
    
    @FXML
    private void updateSelectedModSets() {
        var selectedItems = updatesTable.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            showError("No Selection", "Please select mod sets to update.");
            return;
        }
        
        updateStatusLabel.setText("Updating selected mod sets...");
        updateProgress.setProgress(0);
        
        // Implement basic update logic
        CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate update process
                Platform.runLater(() -> updateProgress.setProgress(0.5));
                Thread.sleep(2000); // Simulate work
                return "Updates completed successfully";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Update interrupted";
            }
        }).thenAcceptAsync(message -> Platform.runLater(() -> {
            updateStatusLabel.setText(message);
            updateProgress.setProgress(1.0);
            // Refresh the updates table
            refreshUpdates();
        }));
    }
    
    @FXML
    private void updateAllModSets() {
        if (updateStatuses.isEmpty()) {
            showError("No Updates", "No updates are available.");
            return;
        }
        
        updateStatusLabel.setText("Updating all mod sets...");
        updateProgress.setProgress(0);
        
        // Implement basic update logic for all mod sets
        CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate update process for all items
                Platform.runLater(() -> updateProgress.setProgress(0.3));
                Thread.sleep(1000);
                Platform.runLater(() -> updateProgress.setProgress(0.7));
                Thread.sleep(1000);
                return "All updates completed successfully";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Update interrupted";
            }
        }).thenAcceptAsync(message -> Platform.runLater(() -> {
            updateStatusLabel.setText(message);
            updateProgress.setProgress(1.0);
            // Refresh the updates table
            refreshUpdates();
        }));
    }
    
    public void navigateToUpdatesTab() {
        // Get the TabPane from the updatesTab
        TabPane tabPane = updatesTab.getTabPane();
        if (tabPane != null) {
            tabPane.getSelectionModel().select(updatesTab);
            // Refresh updates when navigating to the tab
            refreshUpdates();
        }
    }
    
    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
