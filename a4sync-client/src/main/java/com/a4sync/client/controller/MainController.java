package com.a4sync.client.controller;

import com.a4sync.client.config.ClientConfig;
import com.a4sync.client.exception.AuthenticationFailedException;
import com.a4sync.client.model.Repository;
import com.a4sync.client.model.RepositoryModSet;
import com.a4sync.client.service.GameLauncher;
import com.a4sync.client.service.ModManager;
import com.a4sync.client.service.MultiRepositoryService;
import com.a4sync.common.model.Mod;
import com.a4sync.common.model.ModSet;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    
    private final ClientConfig config;
    private final ModManager modManager;
    private final GameLauncher gameLauncher;
    private final MultiRepositoryService multiRepositoryService;
    private final ObservableList<RepositoryModSet> modSets;
    private final ObservableList<Mod> availableMods;
    private final ObservableList<Repository> repositories;
    
    public MainController() {
        this.config = new ClientConfig();
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
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Add Repository");
        nameDialog.setHeaderText("Enter repository name");
        nameDialog.setContentText("Name:");
        
        Optional<String> nameResult = nameDialog.showAndWait();
        if (nameResult.isPresent()) {
            TextInputDialog urlDialog = new TextInputDialog();
            urlDialog.setTitle("Add Repository");
            urlDialog.setHeaderText("Enter repository URL");
            urlDialog.setContentText("URL:");
            
            Optional<String> urlResult = urlDialog.showAndWait();
            if (urlResult.isPresent()) {
                Repository newRepo = new Repository(nameResult.get(), urlResult.get());
                multiRepositoryService.addRepository(newRepo);
                loadRepositories();
                repositoryComboBox.getSelectionModel().select(newRepo);
            }
        }
    }
    
    @FXML
    private void manageRepositories() {
        // TODO: Implement repository management dialog
        showInfo("Repository Management", "Repository management dialog not yet implemented");
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
            // For now, create a local mod set (no repository association)
            // TODO: Allow user to select which repository to create the mod set in
            ModSet modSet = new ModSet();
            modSet.setName(name);
            
            // Create with null repository for local mod sets
            RepositoryModSet repositoryModSet = new RepositoryModSet(null, modSet);
            modSets.add(repositoryModSet);
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
    
    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
