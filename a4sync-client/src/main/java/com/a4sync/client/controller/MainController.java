package com.a4sync.client.controller;

import com.a4sync.client.config.ClientConfig;
import com.a4sync.client.exception.AuthenticationFailedException;
import com.a4sync.client.service.GameLauncher;
import com.a4sync.client.service.ModManager;
import com.a4sync.client.service.RepositoryService;
import com.a4sync.common.model.Mod;
import com.a4sync.common.model.ModSet;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import java.util.Optional;

public class MainController {
    @FXML private ListView<ModSet> modSetList;
    @FXML private TextField searchDirectoryField;
    @FXML private TextField repositoryUrlField;
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
    private final RepositoryService repositoryService;
    private final ObservableList<ModSet> modSets;
    private final ObservableList<Mod> availableMods;
    
    public MainController() {
        this.config = new ClientConfig();
        this.modManager = new ModManager(config);
        this.gameLauncher = new GameLauncher(config);
        this.repositoryService = new RepositoryService(config);
        this.modSets = FXCollections.observableArrayList();
        this.availableMods = FXCollections.observableArrayList();
    }

    @FXML
    private void initialize() {
        modSetList.setItems(modSets);
        availableModsTable.setItems(availableMods);
        
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
    }
    
    private void updateModSetDetails(ModSet modSet) {
        if (modSet != null) {
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
        ModSet selected = modSetList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Mod Set");
            alert.setHeaderText("Delete " + selected.getName());
            alert.setContentText("Are you sure you want to delete this mod set?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                modSets.remove(selected);
            }
        }
    }

    @FXML
    private void refreshRepository() {
        if (repositoryService.getRepositoryUrl() != null) {
            statusLabel.setText("Refreshing repository...");
            repositoryService.getModSets()
                .thenAccept(serverModSets -> {
                    Platform.runLater(() -> {
                        modSets.clear();
                        modSets.addAll(serverModSets);
                        statusLabel.setText("Repository refreshed successfully");
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> 
                        showError("Refresh Failed", 
                            "Failed to refresh repository: " + e.getMessage()));
                    return null;
                });
        }
    }

    @FXML
    private void updateMods() {
        ModSet selected = modSetList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a mod set to update");
            return;
        }

        if (repositoryService.getRepositoryUrl() == null) {
            showError("No Repository", "Please connect to a repository first");
            return;
        }

        downloadProgress.setProgress(0);
        statusLabel.setText("Updating mods...");

        // Count total mods for progress
        int totalMods = selected.getMods().size();
        final int[] completedMods = {0};

        selected.getMods().forEach(mod -> {
            if (!modManager.isModInstalled(mod)) {
                modManager.downloadMod(mod, repositoryService.getRepositoryUrl())
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
            ModSet modSet = new ModSet();
            modSet.setName(name);
            modSets.add(modSet);
        });
    }

    @FXML
    private void launchGame() {
        ModSet selected = modSetList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No mod set selected", "Please select a mod set to launch the game with.");
            return;
        }

        try {
            gameLauncher.launchGame(selected);
        } catch (Exception e) {
            showError("Launch Failed", "Failed to launch game: " + e.getMessage());
        }
    }

    @FXML
    private void connectToRepository() {
        TextInputDialog dialog = new TextInputDialog(config.getServerUrl());
        dialog.setTitle("Connect to Repository");
        dialog.setHeaderText("Enter repository URL");
        dialog.setContentText("URL:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(url -> {
            try {
                statusLabel.setText("Connecting to repository...");
                config.setServerUrl(url);
                repositoryService.setRepositoryUrl(url);
                
                repositoryService.testConnection()
                    .thenCompose(v -> repositoryService.getModSets())
                    .thenAccept(serverModSets -> {
                        Platform.runLater(() -> {
                            modSets.clear();
                            modSets.addAll(serverModSets);
                            repositoryUrlField.setText(url);
                            statusLabel.setText("Connected to repository successfully");
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            Throwable cause = e.getCause();
                            if (cause instanceof AuthenticationFailedException) {
                                showError("Authentication Failed", 
                                    "Invalid repository password. Please check your settings.");
                            } else {
                                showError("Connection Failed", 
                                    "Failed to connect: " + cause.getMessage());
                            }
                            statusLabel.setText("Failed to connect to repository");
                        });
                        return null;
                    });
            } catch (Exception e) {
                showError("Connection Failed", "Invalid repository URL: " + e.getMessage());
            }
        });
    }
    
    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
