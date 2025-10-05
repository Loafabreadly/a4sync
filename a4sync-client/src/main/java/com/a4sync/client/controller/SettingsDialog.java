package com.a4sync.client.controller;

import com.a4sync.client.service.ClientConfig;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

public class SettingsDialog {
    private final Window owner;
    private final ClientConfig config;
    
    public SettingsDialog(Window owner, ClientConfig config) {
        this.owner = owner;
        this.config = config;
    }
    
    public void show() {
        // Game Path
        Optional<Path> gamePath = choosePath("Select Arma 4 Installation Directory", 
            "Select the directory where Arma 4 is installed");
        gamePath.ifPresent(config::saveGamePath);
        
        // Steam Path
        Optional<Path> steamPath = choosePath("Select Steam Executable", 
            "Select the Steam executable (steam.exe on Windows, steam on Linux)");
        steamPath.ifPresent(config::saveSteamPath);
        
        showConfirmation();
    }
    
    private Optional<Path> choosePath(String title, String headerText) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText("Would you like to set this path now?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            File selectedDirectory = chooser.showDialog(owner);
            if (selectedDirectory != null) {
                return Optional.of(selectedDirectory.toPath());
            }
        }
        return Optional.empty();
    }
    
    private void showConfirmation() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings Saved");
        alert.setHeaderText(null);
        alert.setContentText("Settings have been updated successfully.");
        alert.showAndWait();
    }
}
