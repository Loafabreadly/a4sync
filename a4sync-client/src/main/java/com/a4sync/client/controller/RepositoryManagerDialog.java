package com.a4sync.client.controller;

import com.a4sync.client.config.ClientConfig;
import com.a4sync.client.model.Repository;
import com.a4sync.client.model.HealthStatus;
import com.a4sync.client.service.RepositoryService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RepositoryManagerDialog extends Stage {
    private final ClientConfig config;
    private final ObservableList<Repository> repositories;
    private TableView<Repository> repositoryTable;
    private Label totalRepositoriesLabel;
    private Label activeRepositoriesLabel;
    private Label totalSizeLabel;
    
    // Repository details controls
    private TextField nameField;
    private TextField urlField;
    private PasswordField passwordField;
    private CheckBox enabledCheck;
    private CheckBox autoCheckCheck;
    private TextArea notesArea;
    private Label statusLabel;
    private Label lastCheckedLabel;
    private ProgressBar healthProgress;
    
    public RepositoryManagerDialog(ClientConfig config) {
        this.config = config;
        this.repositories = FXCollections.observableArrayList();
        
        initializeUI();
        loadRepositories();
        setTitle("Repository Manager");
        setWidth(900);
        setHeight(600);
        initModality(Modality.APPLICATION_MODAL);
    }
    
    private void initializeUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        
        // Header with statistics
        createStatsHeader(root);
        
        // Main content - split between table and details
        SplitPane mainSplit = new SplitPane();
        mainSplit.setDividerPositions(0.6);
        
        // Left side - Repository table
        VBox leftPanel = createRepositoryTablePanel();
        
        // Right side - Repository details
        VBox rightPanel = createRepositoryDetailsPanel();
        
        mainSplit.getItems().addAll(leftPanel, rightPanel);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);
        
        // Footer buttons
        HBox footerButtons = createFooterButtons();
        
        root.getChildren().addAll(mainSplit, footerButtons);
        setScene(new Scene(root));
    }
    
    private void createStatsHeader(VBox parent) {
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(5);
        statsGrid.getStyleClass().add("stats-header");
        
        totalRepositoriesLabel = new Label("Total Repositories: 0");
        activeRepositoriesLabel = new Label("Active Repositories: 0");
        totalSizeLabel = new Label("Total Size: Calculating...");
        
        statsGrid.add(new Label("Repository Statistics:"), 0, 0, 3, 1);
        statsGrid.add(totalRepositoriesLabel, 0, 1);
        statsGrid.add(activeRepositoriesLabel, 1, 1);
        statsGrid.add(totalSizeLabel, 2, 1);
        
        parent.getChildren().add(statsGrid);
    }
    
    private VBox createRepositoryTablePanel() {
        VBox panel = new VBox(10);
        
        // Table controls
        HBox tableControls = new HBox(10);
        Button addButton = new Button("Add Repository");
        Button removeButton = new Button("Remove");
        Button testAllButton = new Button("Test All Connections");
        Button refreshAllButton = new Button("Refresh All");
        
        addButton.setOnAction(e -> addNewRepository());
        removeButton.setOnAction(e -> removeSelectedRepository());
        testAllButton.setOnAction(e -> testAllConnections());
        refreshAllButton.setOnAction(e -> refreshAllRepositories());
        
        tableControls.getChildren().addAll(addButton, removeButton, new Separator(), 
                                          testAllButton, refreshAllButton);
        
        // Repository table
        repositoryTable = new TableView<>();
        repositoryTable.setItems(repositories);
        repositoryTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldRepo, newRepo) -> updateRepositoryDetails(newRepo));
        
        // Table columns
        TableColumn<Repository, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(120);
        
        TableColumn<Repository, String> urlCol = new TableColumn<>("URL");
        urlCol.setCellValueFactory(new PropertyValueFactory<>("url"));
        urlCol.setPrefWidth(200);
        
        TableColumn<Repository, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> {
            Repository repo = cellData.getValue();
            return new SimpleStringProperty(getStatusText(repo.getHealthStatus()));
        });
        statusCol.setPrefWidth(80);
        
        TableColumn<Repository, String> enabledCol = new TableColumn<>("Enabled");
        enabledCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().isEnabled() ? "Yes" : "No"));
        enabledCol.setPrefWidth(60);
        
        TableColumn<Repository, String> lastCheckedCol = new TableColumn<>("Last Checked");
        lastCheckedCol.setCellValueFactory(cellData -> {
            Repository repo = cellData.getValue();
            if (repo.getLastChecked() != null) {
                return new SimpleStringProperty(
                    repo.getLastChecked().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
            return new SimpleStringProperty("Never");
        });
        lastCheckedCol.setPrefWidth(120);
        
        repositoryTable.getColumns().addAll(nameCol, urlCol, statusCol, enabledCol, lastCheckedCol);
        VBox.setVgrow(repositoryTable, Priority.ALWAYS);
        
        panel.getChildren().addAll(tableControls, repositoryTable);
        return panel;
    }
    
    private VBox createRepositoryDetailsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(0, 0, 0, 15));
        
        Label titleLabel = new Label("Repository Details");
        titleLabel.getStyleClass().add("section-header");
        
        // Form fields
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        
        nameField = new TextField();
        urlField = new TextField();
        passwordField = new PasswordField();
        enabledCheck = new CheckBox();
        autoCheckCheck = new CheckBox();
        notesArea = new TextArea();
        notesArea.setPrefRowCount(3);
        
        form.add(new Label("Name:"), 0, 0);
        form.add(nameField, 1, 0);
        form.add(new Label("URL:"), 0, 1);
        form.add(urlField, 1, 1);
        form.add(new Label("Password:"), 0, 2);
        form.add(passwordField, 1, 2);
        form.add(new Label("Enabled:"), 0, 3);
        form.add(enabledCheck, 1, 3);
        form.add(new Label("Auto Check:"), 0, 4);
        form.add(autoCheckCheck, 1, 4);
        form.add(new Label("Notes:"), 0, 5);
        form.add(notesArea, 1, 5);
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(80);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(col1, col2);
        
        // Status section
        VBox statusSection = new VBox(5);
        Label statusSectionLabel = new Label("Connection Status");
        statusSectionLabel.getStyleClass().add("subsection-header");
        
        statusLabel = new Label("No repository selected");
        lastCheckedLabel = new Label();
        healthProgress = new ProgressBar(0);
        healthProgress.setPrefWidth(200);
        
        HBox statusControls = new HBox(10);
        Button testButton = new Button("Test Connection");
        Button refreshButton = new Button("Refresh Info");
        testButton.setOnAction(e -> testSelectedRepository());
        refreshButton.setOnAction(e -> refreshSelectedRepository());
        statusControls.getChildren().addAll(testButton, refreshButton);
        
        statusSection.getChildren().addAll(statusSectionLabel, statusLabel, 
                                          lastCheckedLabel, healthProgress, statusControls);
        
        // Update button
        Button updateButton = new Button("Update Repository");
        updateButton.setOnAction(e -> updateSelectedRepository());
        
        panel.getChildren().addAll(titleLabel, form, statusSection, updateButton);
        return panel;
    }
    
    private HBox createFooterButtons() {
        HBox footer = new HBox(10);
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        Button importButton = new Button("Import Config");
        Button exportButton = new Button("Export Config");
        Button closeButton = new Button("Close");
        
        importButton.setOnAction(e -> importRepositoryConfig());
        exportButton.setOnAction(e -> exportRepositoryConfig());
        closeButton.setOnAction(e -> close());
        
        footer.getChildren().addAll(importButton, exportButton, new Separator(), closeButton);
        return footer;
    }
    
    private void loadRepositories() {
        repositories.clear();
        repositories.addAll(config.getRepositories());
        updateStats();
    }
    
    private void updateStats() {
        int total = repositories.size();
        int active = (int) repositories.stream().filter(Repository::isEnabled).count();
        
        Platform.runLater(() -> {
            totalRepositoriesLabel.setText("Total Repositories: " + total);
            activeRepositoriesLabel.setText("Active Repositories: " + active);
            calculateTotalSize();
        });
    }
    
    private void calculateTotalSize() {
        Task<String> sizeTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                long totalBytes = 0;
                for (Repository repo : repositories) {
                    if (repo.getTotalSize() > 0) {
                        totalBytes += repo.getTotalSize();
                    }
                }
                return formatSize(totalBytes);
            }
        };
        
        sizeTask.setOnSucceeded(e -> 
            totalSizeLabel.setText("Total Size: " + sizeTask.getValue()));
        
        new Thread(sizeTask).start();
    }
    
    private void updateRepositoryDetails(Repository repository) {
        if (repository == null) {
            clearRepositoryDetails();
            return;
        }
        
        nameField.setText(repository.getName());
        urlField.setText(repository.getUrl());
        passwordField.setText(repository.getPassword() != null ? repository.getPassword() : "");
        enabledCheck.setSelected(repository.isEnabled());
        autoCheckCheck.setSelected(repository.isAutoCheck());
        notesArea.setText(repository.getNotes() != null ? repository.getNotes() : "");
        
        statusLabel.setText(getStatusText(repository.getHealthStatus()));
        if (repository.getLastChecked() != null) {
            lastCheckedLabel.setText("Last checked: " + 
                repository.getLastChecked().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } else {
            lastCheckedLabel.setText("Never checked");
        }
        
        updateHealthProgress(repository.getHealthStatus());
    }
    
    private void clearRepositoryDetails() {
        nameField.clear();
        urlField.clear();
        passwordField.clear();
        enabledCheck.setSelected(false);
        autoCheckCheck.setSelected(false);
        notesArea.clear();
        statusLabel.setText("No repository selected");
        lastCheckedLabel.setText("");
        healthProgress.setProgress(0);
    }
    
    private void addNewRepository() {
        RepositoryEditDialog dialog = new RepositoryEditDialog(null);
        Optional<Repository> result = dialog.showAndWait();
        result.ifPresent(repository -> {
            config.addRepository(repository);
            loadRepositories();
        });
    }
    
    private void removeSelectedRepository() {
        Repository selected = repositoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Repository");
        confirm.setHeaderText("Remove " + selected.getName() + "?");
        confirm.setContentText("This will remove the repository from your configuration. Downloaded mods will not be deleted.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                config.removeRepository(selected.getId());
                loadRepositories();
                clearRepositoryDetails();
            }
        });
    }
    
    private void updateSelectedRepository() {
        Repository selected = repositoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        selected.setName(nameField.getText());
        selected.setUrl(urlField.getText());
        selected.setPassword(passwordField.getText().isEmpty() ? null : passwordField.getText());
        selected.setEnabled(enabledCheck.isSelected());
        selected.setAutoCheck(autoCheckCheck.isSelected());
        selected.setNotes(notesArea.getText().isEmpty() ? null : notesArea.getText());
        
        config.updateRepository(selected);
        repositoryTable.refresh();
        updateStats();
    }
    
    private void testSelectedRepository() {
        Repository selected = repositoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        testRepositoryConnection(selected);
    }
    
    private void refreshSelectedRepository() {
        Repository selected = repositoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        refreshRepositoryInfo(selected);
    }
    
    private void testAllConnections() {
        for (Repository repo : repositories) {
            if (repo.isEnabled()) {
                testRepositoryConnection(repo);
            }
        }
    }
    
    private void refreshAllRepositories() {
        for (Repository repo : repositories) {
            if (repo.isEnabled()) {
                refreshRepositoryInfo(repo);
            }
        }
    }
    
    private void testRepositoryConnection(Repository repository) {
        statusLabel.setText("Testing connection...");
        healthProgress.setProgress(-1); // Indeterminate progress
        
        CompletableFuture.supplyAsync(() -> {
            try {
                RepositoryService service = new RepositoryService(createConfigForRepository(repository));
                return service.testConnection();
            } catch (Exception e) {
                log.error("Failed to test repository connection", e);
                return HealthStatus.ERROR;
            }
        }).thenAccept(status -> Platform.runLater(() -> {
            repository.setHealthStatus(status);
            repository.setLastChecked(java.time.LocalDateTime.now());
            config.updateRepository(repository);
            
            updateRepositoryDetails(repository);
            repositoryTable.refresh();
        }));
    }
    
    private void refreshRepositoryInfo(Repository repository) {
        CompletableFuture.runAsync(() -> {
            try {
                RepositoryService service = new RepositoryService(createConfigForRepository(repository));
                long size = service.getRepositorySize();
                
                Platform.runLater(() -> {
                    repository.setTotalSize(size);
                    repository.setLastChecked(java.time.LocalDateTime.now());
                    config.updateRepository(repository);
                    updateStats();
                    repositoryTable.refresh();
                });
            } catch (Exception e) {
                log.error("Failed to refresh repository info", e);
            }
        });
    }
    
    private ClientConfig createConfigForRepository(Repository repository) {
        ClientConfig tempConfig = new ClientConfig();
        tempConfig.setServerUrl(repository.getUrl());
        tempConfig.setRepositoryPassword(repository.getPassword());
        tempConfig.setUseAuthentication(repository.getPassword() != null && !repository.getPassword().isEmpty());
        return tempConfig;
    }
    
    private void importRepositoryConfig() {
        // TODO: Implement config import
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Import Config");
        info.setContentText("Import feature coming soon!");
        info.showAndWait();
    }
    
    private void exportRepositoryConfig() {
        // TODO: Implement config export
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Export Config");
        info.setContentText("Export feature coming soon!");
        info.showAndWait();
    }
    
    private String getStatusText(HealthStatus status) {
        if (status == null) return "Unknown";
        return switch (status) {
            case HEALTHY -> "Online";
            case DEGRADED -> "Degraded";
            case ERROR -> "Offline";
            case UNKNOWN -> "Unknown";
        };
    }
    
    private void updateHealthProgress(HealthStatus status) {
        if (status == null) {
            healthProgress.setProgress(0);
            return;
        }
        
        double progress = switch (status) {
            case HEALTHY -> 1.0;
            case DEGRADED -> 0.5;
            case ERROR -> 0.0;
            case UNKNOWN -> 0.0;
        };
        
        healthProgress.setProgress(progress);
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}