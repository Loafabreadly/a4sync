package com.a4sync.client.controller;

import com.a4sync.client.config.ClientConfig;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

public class SettingsDialog extends Stage {
    private final ClientConfig config;
    private final TextField steamPathField;
    private final TextField gamePathField;
    private final CheckBox useAuthCheck;
    private final PasswordField repositoryPasswordField;
    private final TextField maxRetriesField;
    private final TextField retryDelayField;
    
    public SettingsDialog(Window owner, ClientConfig config) {
        this.config = config;

        // Configure dialog
        initModality(Modality.WINDOW_MODAL);
        initOwner(owner);
        setTitle("Settings");
        setResizable(false);

        // Create UI components
        steamPathField = new TextField();
        gamePathField = new TextField();
        useAuthCheck = new CheckBox("Use Authentication");
        repositoryPasswordField = new PasswordField();
        maxRetriesField = new TextField();
        retryDelayField = new TextField();
        
        if (config.getSteamPath() != null) {
            steamPathField.setText(config.getSteamPath().toString());
        }
        if (config.getGamePath() != null) {
            gamePathField.setText(config.getGamePath().toString());
        }
        useAuthCheck.setSelected(config.isUseAuthentication());
        repositoryPasswordField.setText(config.getRepositoryPassword());
        repositoryPasswordField.setDisable(!config.isUseAuthentication());
        maxRetriesField.setText(String.valueOf(config.getMaxRetries()));
        retryDelayField.setText(String.valueOf(config.getRetryDelayMs()));
        
        setupLayout();
    }
    
    private void setupLayout() {
        // Setup layout
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

        Button steamPathButton = new Button("Browse...");
        Button gamePathButton = new Button("Browse...");
        Button saveButton = new Button("Save");
        Button cancelButton = new Button("Cancel");

        // Add components to grid
        int row = 0;
        grid.add(new Label("Steam Path:"), 0, row);
        grid.add(steamPathField, 1, row);
        grid.add(steamPathButton, 2, row++);

        grid.add(new Label("Game Path:"), 0, row);
        grid.add(gamePathField, 1, row);
        grid.add(gamePathButton, 2, row++);

        grid.add(useAuthCheck, 0, row++, 2, 1);
        
        grid.add(new Label("Repository Password:"), 0, row);
        grid.add(repositoryPasswordField, 1, row++, 2, 1);

        grid.add(new Label("Max Retries:"), 0, row);
        grid.add(maxRetriesField, 1, row++, 2, 1);

        grid.add(new Label("Retry Delay (ms):"), 0, row);
        grid.add(retryDelayField, 1, row++, 2, 1);

        grid.add(saveButton, 1, row);
        grid.add(cancelButton, 2, row);

        // Setup actions
        steamPathButton.setOnAction(e -> choosePath(steamPathField));
        gamePathButton.setOnAction(e -> choosePath(gamePathField));
        saveButton.setOnAction(e -> saveSettings());
        cancelButton.setOnAction(e -> close());
        useAuthCheck.selectedProperty().addListener((obs, oldVal, newVal) -> 
            repositoryPasswordField.setDisable(!newVal));

        Scene scene = new Scene(grid);
        setScene(scene);
    }
    
    private void choosePath(TextField field) {
        DirectoryChooser chooser = new DirectoryChooser();
        File currentPath = new File(field.getText());
        if (currentPath.exists()) {
            chooser.setInitialDirectory(currentPath);
        }

        File selected = chooser.showDialog(getScene().getWindow());
        if (selected != null) {
            field.setText(selected.getAbsolutePath());
        }
    }
    
    private void saveSettings() {
        // Validate and save paths
        String steamPath = steamPathField.getText();
        if (!steamPath.isEmpty()) {
            config.setSteamPath(Path.of(steamPath));
        }

        String gamePath = gamePathField.getText();
        if (!gamePath.isEmpty()) {
            config.setGamePath(Path.of(gamePath));
        }

        // Save authentication settings
        config.setUseAuthentication(useAuthCheck.isSelected());
        config.setRepositoryPassword(repositoryPasswordField.getText());

        // Save retry settings
        try {
            config.setMaxRetries(Integer.parseInt(maxRetriesField.getText()));
            config.setRetryDelayMs(Long.parseLong(retryDelayField.getText()));
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText(null);
            alert.setContentText("Please enter valid numbers for max retries and retry delay.");
            alert.showAndWait();
            return;
        }

        close();
    }
}
