package com.a4sync.client.controller;

import com.a4sync.client.config.ClientConfig;
import com.a4sync.common.model.GameOptions;
import com.a4sync.common.model.GameType;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.io.File;
import java.nio.file.Path;

public class SettingsDialog extends Stage {
    private final ClientConfig config;
    private final TextField steamPathField;
    private final TextField gamePathField;
    private final CheckBox useAuthCheck;
    private final PasswordField repositoryPasswordField;
    private final TextField maxRetriesField;
    private final TextField retryDelayField;
    
    // Game launch settings
    private final ComboBox<GameType> defaultGameTypeComboBox;
    private final TextField defaultProfileField;
    private final CheckBox defaultNoSplashCheckBox;
    private final TextArea defaultAdditionalParamsArea;
    
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
        
        // Game launch components
        defaultGameTypeComboBox = new ComboBox<>();
        defaultGameTypeComboBox.getItems().addAll(GameType.values());
        defaultProfileField = new TextField();
        defaultNoSplashCheckBox = new CheckBox("Disable Splash Screen by Default");
        defaultAdditionalParamsArea = new TextArea();
        defaultAdditionalParamsArea.setPrefRowCount(3);
        
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
        
        // Initialize game options
        GameOptions defaultOptions = config.getDefaultGameOptions();
        if (defaultOptions != null) {
            defaultGameTypeComboBox.setValue(defaultOptions.getGameType());
            defaultProfileField.setText(defaultOptions.getProfileName() != null ? 
                defaultOptions.getProfileName() : "");
            defaultNoSplashCheckBox.setSelected(defaultOptions.isNoSplash());
            defaultAdditionalParamsArea.setText(defaultOptions.getAdditionalParameters() != null ? 
                defaultOptions.getAdditionalParameters() : "");
        } else {
            defaultGameTypeComboBox.setValue(GameType.ARMA_4);
        }
        
        setupLayout();
    }
    
    private void setupLayout() {
        TabPane tabPane = new TabPane();
        
        // General settings tab
        Tab generalTab = new Tab("General");
        generalTab.setClosable(false);
        GridPane generalGrid = createGeneralSettingsGrid();
        generalTab.setContent(generalGrid);
        
        // Game launch settings tab  
        Tab gameTab = new Tab("Game Launch");
        gameTab.setClosable(false);
        GridPane gameGrid = createGameSettingsGrid();
        gameTab.setContent(gameGrid);
        
        tabPane.getTabs().addAll(generalTab, gameTab);
        
        // Buttons
        Button saveButton = new Button("Save");
        Button cancelButton = new Button("Cancel");
        
        GridPane buttonGrid = new GridPane();
        buttonGrid.setHgap(10);
        buttonGrid.add(saveButton, 0, 0);
        buttonGrid.add(cancelButton, 1, 0);
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.getChildren().addAll(tabPane, buttonGrid);
        
        // Setup actions
        saveButton.setOnAction(e -> saveSettings());
        cancelButton.setOnAction(e -> close());
        useAuthCheck.selectedProperty().addListener((obs, oldVal, newVal) -> 
            repositoryPasswordField.setDisable(!newVal));

        Scene scene = new Scene(root, 600, 450);
        setScene(scene);
    }
    
    private GridPane createGeneralSettingsGrid() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

        Button steamPathButton = new Button("Browse...");
        Button gamePathButton = new Button("Browse...");
        
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
        
        // Setup path browser actions
        steamPathButton.setOnAction(e -> choosePath(steamPathField));
        gamePathButton.setOnAction(e -> choosePath(gamePathField));
        
        return grid;
    }
    
    private GridPane createGameSettingsGrid() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);
        
        int row = 0;
        
        grid.add(new Label("Default Game:"), 0, row);
        grid.add(defaultGameTypeComboBox, 1, row++, 2, 1);
        
        grid.add(new Label("Default Profile:"), 0, row);
        defaultProfileField.setPromptText("Leave empty for default");
        grid.add(defaultProfileField, 1, row++, 2, 1);
        
        grid.add(defaultNoSplashCheckBox, 0, row++, 3, 1);
        
        grid.add(new Label("Default Additional Parameters:"), 0, row++, 3, 1);
        defaultAdditionalParamsArea.setPromptText("Enter additional launch parameters...");
        grid.add(defaultAdditionalParamsArea, 0, row++, 3, 1);
        
        return grid;
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

        // Save authentication settings (directly to fields since Lombok provides setters)
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
        
        // Save game options
        GameOptions gameOptions = new GameOptions();
        gameOptions.setGameType(defaultGameTypeComboBox.getValue());
        gameOptions.setProfileName(defaultProfileField.getText().trim());
        gameOptions.setNoSplash(defaultNoSplashCheckBox.isSelected());
        gameOptions.setAdditionalParameters(defaultAdditionalParamsArea.getText().trim());
        config.saveDefaultGameOptions(gameOptions);

        close();
    }
}
