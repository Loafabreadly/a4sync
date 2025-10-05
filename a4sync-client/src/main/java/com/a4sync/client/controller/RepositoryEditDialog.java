package com.a4sync.client.controller;

import com.a4sync.client.model.Repository;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class RepositoryEditDialog extends Dialog<Repository> {
    private final Repository repository;
    
    public RepositoryEditDialog(Repository repository) {
        this.repository = repository;
        
        setTitle(repository == null ? "Add Repository" : "Edit Repository");
        setHeaderText(repository == null ? "Enter new repository details" : "Edit repository details");
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField();
        TextField urlField = new TextField();
        PasswordField passwordField = new PasswordField();
        CheckBox enabledCheck = new CheckBox();
        CheckBox autoCheckCheck = new CheckBox();
        TextArea notesArea = new TextArea();
        
        // Set default values
        enabledCheck.setSelected(true);
        autoCheckCheck.setSelected(true);
        notesArea.setPrefRowCount(3);
        
        if (repository != null) {
            nameField.setText(repository.getName());
            urlField.setText(repository.getUrl());
            passwordField.setText(repository.getPassword() != null ? repository.getPassword() : "");
            enabledCheck.setSelected(repository.isEnabled());
            autoCheckCheck.setSelected(repository.isAutoCheck());
            notesArea.setText(repository.getNotes() != null ? repository.getNotes() : "");
        }
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("URL:"), 0, 1);
        grid.add(urlField, 1, 1);
        grid.add(new Label("Password:"), 0, 2);
        grid.add(passwordField, 1, 2);
        grid.add(new Label("Enabled:"), 0, 3);
        grid.add(enabledCheck, 1, 3);
        grid.add(new Label("Auto Check:"), 0, 4);
        grid.add(autoCheckCheck, 1, 4);
        grid.add(new Label("Notes:"), 0, 5);
        grid.add(notesArea, 1, 5);
        
        getDialogPane().setContent(grid);
        
        // Validation
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        
        nameField.textProperty().addListener((observable, oldValue, newValue) -> 
            saveButton.setDisable(newValue.trim().isEmpty() || urlField.getText().trim().isEmpty()));
        urlField.textProperty().addListener((observable, oldValue, newValue) -> 
            saveButton.setDisable(newValue.trim().isEmpty() || nameField.getText().trim().isEmpty()));
        
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Repository repo = repository != null ? repository : new Repository();
                repo.setName(nameField.getText().trim());
                repo.setUrl(urlField.getText().trim());
                repo.setPassword(passwordField.getText().isEmpty() ? null : passwordField.getText());
                repo.setEnabled(enabledCheck.isSelected());
                repo.setAutoCheck(autoCheckCheck.isSelected());
                repo.setNotes(notesArea.getText().isEmpty() ? null : notesArea.getText());
                
                if (repository == null) {
                    repo.setId(java.util.UUID.randomUUID().toString());
                }
                
                return repo;
            }
            return null;
        });
    }
}