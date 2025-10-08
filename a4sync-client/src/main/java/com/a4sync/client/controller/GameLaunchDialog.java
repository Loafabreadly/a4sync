package com.a4sync.client.controller;

import com.a4sync.common.model.GameOptions;
import com.a4sync.common.model.GameType;
import com.a4sync.common.model.ModSet;
import com.a4sync.client.config.ClientConfig;
import com.a4sync.client.service.GameLauncher;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class GameLaunchDialog extends Stage {
    
    private final ClientConfig config;
    private final GameLauncher gameLauncher;
    private final ModSet modSet;
    
    private ComboBox<GameType> gameTypeComboBox;
    private TextField profileField;
    private CheckBox noSplashCheckBox;
    private TextArea additionalParamsArea;
    private CheckBox saveAsDefaultCheckBox;
    
    private boolean launched = false;

    public GameLaunchDialog(Window owner, ClientConfig config, ModSet modSet) {
        this.config = config;
        this.gameLauncher = new GameLauncher(config);
        this.modSet = modSet;
        
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setTitle("Launch Game - " + (modSet != null ? modSet.getName() : "Default"));
        setResizable(false);
        
        createUI();
        loadCurrentOptions();
    }
    
    private void createUI() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        
        // Game selection
        Label gameLabel = new Label("Select Game:");
        gameLabel.getStyleClass().add("header-label");
        
        gameTypeComboBox = new ComboBox<>();
        gameTypeComboBox.getItems().addAll(GameType.values());
        gameTypeComboBox.setPrefWidth(200);
        
        // Game options
        Label optionsLabel = new Label("Launch Options:");
        optionsLabel.getStyleClass().add("header-label");
        
        GridPane optionsGrid = new GridPane();
        optionsGrid.setHgap(10);
        optionsGrid.setVgap(10);
        
        // Profile name
        optionsGrid.add(new Label("Profile Name:"), 0, 0);
        profileField = new TextField();
        profileField.setPromptText("Leave empty for default");
        optionsGrid.add(profileField, 1, 0);
        
        // No splash
        noSplashCheckBox = new CheckBox("Disable Splash Screen");
        optionsGrid.add(noSplashCheckBox, 0, 1, 2, 1);
        
        // Additional parameters
        optionsGrid.add(new Label("Additional Parameters:"), 0, 2);
        additionalParamsArea = new TextArea();
        additionalParamsArea.setPrefRowCount(3);
        additionalParamsArea.setPrefColumnCount(30);
        additionalParamsArea.setPromptText("Enter additional launch parameters...");
        optionsGrid.add(additionalParamsArea, 1, 2);
        
        // Save as default option
        saveAsDefaultCheckBox = new CheckBox("Save these settings as default for new mod sets");
        
        // Mod set info
        Label modSetLabel = new Label("Mod Set: " + 
            (modSet != null ? modSet.getName() + " (" + 
                (modSet.getMods() != null ? modSet.getMods().size() : 0) + " mods)" : "None"));
        modSetLabel.getStyleClass().add("info-label");
        
        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button launchButton = new Button("Launch Game");
        launchButton.setPrefWidth(100);
        launchButton.setOnAction(e -> launchGame());
        launchButton.getStyleClass().add("primary-button");
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(80);
        cancelButton.setOnAction(e -> close());
        
        buttonBox.getChildren().addAll(cancelButton, launchButton);
        
        // Add all to root
        root.getChildren().addAll(
            gameLabel,
            gameTypeComboBox,
            new Separator(),
            optionsLabel,
            optionsGrid,
            new Separator(),
            saveAsDefaultCheckBox,
            modSetLabel,
            buttonBox
        );
        
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/repository.css").toExternalForm());
        setScene(scene);
    }
    
    private void loadCurrentOptions() {
        // Load from client-side configuration only
        GameOptions options = config.getDefaultGameOptions();
        
        if (options != null) {
            gameTypeComboBox.setValue(options.getGameType() != null ? 
                options.getGameType() : GameType.ARMA_4);
            profileField.setText(options.getProfileName() != null ? 
                options.getProfileName() : "");
            noSplashCheckBox.setSelected(options.isNoSplash());
            additionalParamsArea.setText(options.getAdditionalParameters() != null ? 
                options.getAdditionalParameters() : "");
        } else {
            gameTypeComboBox.setValue(GameType.ARMA_4);
        }
    }
    
    private void launchGame() {
        try {
            GameType selectedGame = gameTypeComboBox.getValue();
            if (selectedGame == null) {
                showError("Please select a game to launch.");
                return;
            }
            
            // Check Steam path is configured
            if (config.getSteamPath() == null) {
                showError("Steam path is not configured. Please configure it in Settings.");
                return;
            }
            
            // Create game options from UI
            GameOptions launchOptions = new GameOptions();
            launchOptions.setGameType(selectedGame);
            launchOptions.setProfileName(profileField.getText().trim());
            launchOptions.setNoSplash(noSplashCheckBox.isSelected());
            launchOptions.setAdditionalParameters(additionalParamsArea.getText().trim());
            
            // Save as default if requested
            if (saveAsDefaultCheckBox.isSelected()) {
                config.saveDefaultGameOptions(launchOptions);
            }
            
            // Update modset options if we have a modset
            if (modSet != null) {
                modSet.setGameOptions(launchOptions);
            }
            
            // Launch the game
            if (modSet != null) {
                gameLauncher.launchGameWithType(modSet, selectedGame);
            } else {
                // Create temporary modset for launch
                ModSet tempModSet = new ModSet();
                tempModSet.setGameOptions(launchOptions);
                gameLauncher.launchGameWithType(tempModSet, selectedGame);
            }
            
            launched = true;
            showInfo("Launching " + selectedGame.getDisplayName() + " via Steam...");
            close();
            
        } catch (IOException e) {
            log.error("Failed to launch game", e);
            showError("Failed to launch game: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during game launch", e);
            showError("Unexpected error: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(this);
        alert.setTitle("Launch Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(this);
        alert.setTitle("Game Launch");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
    
    public boolean wasLaunched() {
        return launched;
    }
}