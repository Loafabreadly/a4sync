package com.a4sync.client.controller;

import com.a4sync.client.config.RepositoryConfig;
import com.a4sync.client.service.RepositoryManager;
import com.a4sync.client.service.RepositoryManagerFactory;
import com.a4sync.common.model.ModSet;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
        
        // Header with name and status
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(repo.getName());
        nameLabel.getStyleClass().add("repository-name");
        
        Label statusLabel = new Label("Checking...");
        statusLabel.getStyleClass().add("repository-status");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button testConnectionButton = new Button("Test Connection");
        testConnectionButton.setOnAction(e -> testRepositoryConnection(repo, statusLabel));
        
        header.getChildren().addAll(nameLabel, spacer, statusLabel, testConnectionButton);
        
        Label urlLabel = new Label(repo.getUrl());
        urlLabel.getStyleClass().add("repository-url");
        
        // Repository info labels
        Label sizeLabel = new Label("Size: Calculating...");
        sizeLabel.getStyleClass().add("repository-info");
        
        Label lastCheckedLabel = new Label();
        lastCheckedLabel.getStyleClass().add("repository-info");
        updateLastCheckedLabel(repo, lastCheckedLabel);
        
        if (repo.getLastError() != null && !repo.getLastError().isEmpty()) {
            Label errorLabel = new Label("Last Error: " + repo.getLastError());
            errorLabel.getStyleClass().add("repository-error");
            entry.getChildren().add(errorLabel);
        }
        
        // Controls
        HBox controls = new HBox(10);
        controls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
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
        
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshRepositoryInfo(repo, sizeLabel, lastCheckedLabel));
        
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
        
        controls.getChildren().addAll(enabledCheck, checkOnStartupBox, refreshButton, removeButton);
        
        entry.getChildren().addAll(header, urlLabel, sizeLabel, lastCheckedLabel, controls);
        repositoryList.getChildren().add(entry);
        
        // Automatically test connection and get size on load
        testRepositoryConnection(repo, statusLabel);
        refreshRepositoryInfo(repo, sizeLabel, lastCheckedLabel);
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
            
            // Show success message
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Repository Added");
            success.setHeaderText("Repository Successfully Added");
            success.setContentText("Repository '" + config.getName() + "' has been added successfully!");
            success.showAndWait();
        });
    }
    
    private void testRepositoryConnection(RepositoryConfig repo, Label statusLabel) {
        Platform.runLater(() -> {
            statusLabel.setText("Testing...");
            statusLabel.getStyleClass().removeAll("repository-status-success", "repository-status-error", "repository-status-degraded");
            statusLabel.getStyleClass().add("repository-status-testing");
        });
        
        CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(repo.getUrl() + "/api/v1/health"))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();
                
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    // Parse the JSON response to get the health status
                    String body = response.body();
                    if (body.contains("\"status\":\"UP\"")) {
                        return "UP";
                    } else if (body.contains("\"status\":\"DEGRADED\"")) {
                        return "DEGRADED";
                    } else if (body.contains("\"status\":\"DOWN\"")) {
                        return "DOWN";
                    } else {
                        return "UNKNOWN";
                    }
                } else {
                    return "DOWN";
                }
            } catch (Exception e) {
                return "DOWN";
            }
        }).thenAccept(healthStatus -> Platform.runLater(() -> {
            switch (healthStatus) {
                case "UP":
                    statusLabel.setText("Healthy ✓");
                    statusLabel.getStyleClass().removeAll("repository-status-testing", "repository-status-error", "repository-status-degraded");
                    statusLabel.getStyleClass().add("repository-status-success");
                    repo.setLastError(null);
                    break;
                case "DEGRADED":
                    statusLabel.setText("Degraded ⚠");
                    statusLabel.getStyleClass().removeAll("repository-status-testing", "repository-status-error", "repository-status-success");
                    statusLabel.getStyleClass().add("repository-status-degraded");
                    repo.setLastError("Server is in degraded state");
                    break;
                case "DOWN":
                case "UNKNOWN":
                default:
                    statusLabel.setText("Unhealthy ✗");
                    statusLabel.getStyleClass().removeAll("repository-status-testing", "repository-status-success", "repository-status-degraded");
                    statusLabel.getStyleClass().add("repository-status-error");
                    repo.setLastError("Health check failed - status: " + healthStatus);
                    break;
            }
            repo.setLastChecked(Instant.now());
            RepositoryManagerFactory.getInstance().updateRepository(repo);
        }));
    }
    
    private void refreshRepositoryInfo(RepositoryConfig repo, Label sizeLabel, Label lastCheckedLabel) {
        Platform.runLater(() -> sizeLabel.setText("Size: Calculating..."));
        
        CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                
                // First try to get repository info which should include size statistics
                HttpRequest infoRequest = HttpRequest.newBuilder()
                    .uri(URI.create(repo.getUrl() + "/api/v1/repository/info"))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .GET()
                    .build();
                
                HttpResponse<String> infoResponse = client.send(infoRequest, HttpResponse.BodyHandlers.ofString());
                if (infoResponse.statusCode() == 200) {
                    // Try to parse repository info to get actual size
                    String body = infoResponse.body();
                    // Simple JSON parsing for totalSize field
                    if (body.contains("\"totalSize\"")) {
                        try {
                            String sizeStr = body.substring(body.indexOf("\"totalSize\":") + 12);
                            sizeStr = sizeStr.substring(0, sizeStr.indexOf(',') != -1 ? sizeStr.indexOf(',') : sizeStr.indexOf('}'));
                            return Long.parseLong(sizeStr.trim());
                        } catch (Exception e) {
                            // Fall back to modsets endpoint for estimation
                        }
                    }
                }
                
                // Fallback: use modsets endpoint for estimation
                HttpRequest modsetsRequest = HttpRequest.newBuilder()
                    .uri(URI.create(repo.getUrl() + "/api/v1/modsets"))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .GET()
                    .build();
                
                HttpResponse<String> modsetsResponse = client.send(modsetsRequest, HttpResponse.BodyHandlers.ofString());
                if (modsetsResponse.statusCode() == 200) {
                    // Estimate based on modsets count and response size
                    String body = modsetsResponse.body();
                    long estimatedSize = body.length() * 1000; // Rough estimation
                    return estimatedSize;
                }
                return 0L;
            } catch (Exception e) {
                return 0L;
            }
        }).thenAccept(size -> Platform.runLater(() -> {
            if (size > 0) {
                sizeLabel.setText("Size: " + formatSize(size));
            } else {
                sizeLabel.setText("Size: Unknown");
            }
            updateLastCheckedLabel(repo, lastCheckedLabel);
        }));
    }
    
    private void updateLastCheckedLabel(RepositoryConfig repo, Label lastCheckedLabel) {
        if (repo.getLastChecked() != null) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(repo.getLastChecked(), ZoneId.systemDefault());
            String formatted = dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
            lastCheckedLabel.setText("Last Checked: " + formatted);
        } else {
            lastCheckedLabel.setText("Last Checked: Never");
        }
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}