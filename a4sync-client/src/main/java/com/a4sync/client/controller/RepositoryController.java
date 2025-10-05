package com.a4sync.client.controller;

import com.a4sync.client.config.RepositoryConfig;
import com.a4sync.client.service.RepositoryManager;
import com.a4sync.client.service.RepositoryManagerFactory;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class RepositoryController {
    @FXML
    private VBox repositoryList;
    
    @FXML
    private Button addRepositoryButton;
    
    @FXML
    private void initialize() {
        loadRepositories();
    }
    
    private void loadRepositories() {
        RepositoryManager manager = RepositoryManagerFactory.getInstance();
        repositoryList.getChildren().clear();
        
        for (RepositoryConfig repo : manager.getRepositories()) {
            createRepositoryEntry(repo);
        }
    }
    
    private void createRepositoryEntry(RepositoryConfig repo) {
        VBox entry = new VBox(5);
        entry.getStyleClass().add("repository-entry");
        
        Label nameLabel = new Label(repo.getName());
        nameLabel.getStyleClass().add("repository-name");
        
        Label urlLabel = new Label(repo.getUrl());
        urlLabel.getStyleClass().add("repository-url");
        
        CheckBox enabledCheck = new CheckBox("Enabled");
        enabledCheck.setSelected(repo.isEnabled());
        enabledCheck.selectedProperty().addListener((obs, old, newVal) -> {
            repo.setEnabled(newVal);
            RepositoryManagerFactory.getInstance().updateRepository(repo);
        });
        
        CheckBox checkOnStartupBox = new CheckBox("Check on startup");
        checkOnStartupBox.setSelected(repo.isCheckOnStartup());
        checkOnStartupBox.selectedProperty().addListener((obs, old, newVal) -> {
            repo.setCheckOnStartup(newVal);
            RepositoryManagerFactory.getInstance().updateRepository(repo);
        });
        
        Button removeButton = new Button("Remove");
        removeButton.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Remove Repository");
            confirm.setHeaderText("Remove " + repo.getName() + "?");
            confirm.setContentText("This will remove the repository configuration but not delete any downloaded mods.");
            
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    RepositoryManagerFactory.getInstance().removeRepository(repo);
                    loadRepositories();
                }
            });
        });
        
        entry.getChildren().addAll(nameLabel, urlLabel, enabledCheck, checkOnStartupBox, removeButton);
        repositoryList.getChildren().add(entry);
    }
    
    @FXML
    private void onAddRepository() {
        Dialog<RepositoryConfig> dialog = new Dialog<>();
        dialog.setTitle("Add Repository");
        dialog.setHeaderText("Enter repository details");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        TextField nameField = new TextField();
        TextField urlField = new TextField();
        PasswordField passField = new PasswordField();
        CheckBox enabledCheck = new CheckBox();
        enabledCheck.setSelected(true);
        CheckBox checkOnStartupBox = new CheckBox();
        checkOnStartupBox.setSelected(true);
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("URL:"), 0, 1);
        grid.add(urlField, 1, 1);
        grid.add(new Label("Password:"), 0, 2);
        grid.add(passField, 1, 2);
        grid.add(new Label("Enabled:"), 0, 3);
        grid.add(enabledCheck, 1, 4);
        grid.add(new Label("Check on startup:"), 0, 5);
        grid.add(checkOnStartupBox, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                RepositoryConfig config = new RepositoryConfig();
                config.setName(nameField.getText());
                config.setUrl(urlField.getText());
                config.setPassword(passField.getText());
                config.setEnabled(enabledCheck.isSelected());
                config.setCheckOnStartup(checkOnStartupBox.isSelected());
                return config;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(config -> {
            RepositoryManagerFactory.getInstance().addRepository(config);
            loadRepositories();
        });
    }
}