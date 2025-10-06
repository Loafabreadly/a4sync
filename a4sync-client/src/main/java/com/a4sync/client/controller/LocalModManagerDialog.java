package com.a4sync.client.controller;

import com.a4sync.client.config.ClientConfig;
import com.a4sync.client.service.LocalModRepository;
import com.a4sync.client.service.ModManager;
import com.a4sync.common.model.Mod;
import com.a4sync.common.model.ModSet;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class LocalModManagerDialog extends Stage {
    
    // Inner class for local mod set information
    public static class LocalModSetInfo {
        private String name;
        private String path;
        private int modCount;
        private long size;
        private LocalDateTime lastModified;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public int getModCount() { return modCount; }
        public void setModCount(int modCount) { this.modCount = modCount; }
        
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        
        public LocalDateTime getLastModified() { return lastModified; }
        public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
        
        public String getFormattedSize() {
            if (size == 0) return "0 B";
            
            String[] units = {"B", "KB", "MB", "GB"};
            int unitIndex = 0;
            double sizeDouble = size;
            
            while (sizeDouble >= 1024 && unitIndex < units.length - 1) {
                sizeDouble /= 1024;
                unitIndex++;
            }
            
            return String.format("%.1f %s", sizeDouble, units[unitIndex]);
        }
    }

    private final ClientConfig config;
    private final ModManager modManager;
    private final LocalModRepository localModRepo;
    
    private TabPane mainTabPane;
    private TableView<LocalModInfo> localModsTable;
    private TableView<LocalModSetInfo> localModSetsTable;
    private ObservableList<LocalModInfo> localMods;
    private ObservableList<LocalModSetInfo> localModSets;
    
    // Statistics labels
    private Label totalModsLabel;
    private Label totalModSetsLabel;
    private Label totalSizeLabel;
    private Label lastScanLabel;
    
    // Mod details panel
    private TextArea modDetailsArea;
    private ListView<String> modFilesList;
    
    public LocalModManagerDialog(ClientConfig config) {
        this.config = config;
        this.modManager = new ModManager(config);
        this.localModRepo = new LocalModRepository(config);
        this.localMods = FXCollections.observableArrayList();
        this.localModSets = FXCollections.observableArrayList();
        
        initializeUI();
        scanLocalContent();
        
        setTitle("Local Mod Manager");
        setWidth(1000);
        setHeight(700);
        initModality(Modality.APPLICATION_MODAL);
    }
    
    private void initializeUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        
        // Header with statistics and controls
        createHeaderPanel(root);
        
        // Main content tabs
        mainTabPane = new TabPane();
        mainTabPane.getTabs().addAll(
            createLocalModsTab(),
            createLocalModSetsTab(),
            createDirectoryManagerTab()
        );
        VBox.setVgrow(mainTabPane, Priority.ALWAYS);
        
        // Footer buttons
        HBox footer = createFooterButtons();
        
        root.getChildren().addAll(mainTabPane, footer);
        setScene(new Scene(root));
    }
    
    private void createHeaderPanel(VBox parent) {
        // Statistics panel
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(5);
        statsGrid.getStyleClass().add("stats-header");
        
        totalModsLabel = new Label("Total Mods: 0");
        totalModSetsLabel = new Label("Total Mod Sets: 0");
        totalSizeLabel = new Label("Total Size: Calculating...");
        lastScanLabel = new Label("Last Scan: Never");
        
        statsGrid.add(new Label("Local Content Statistics:"), 0, 0, 4, 1);
        statsGrid.add(totalModsLabel, 0, 1);
        statsGrid.add(totalModSetsLabel, 1, 1);
        statsGrid.add(totalSizeLabel, 2, 1);
        statsGrid.add(lastScanLabel, 3, 1);
        
        // Control buttons
        HBox controls = new HBox(10);
        controls.setPadding(new Insets(10, 0, 0, 0));
        
        Button scanButton = new Button("Scan All Directories");
        Button cleanupButton = new Button("Cleanup Orphaned Files");
        Button settingsButton = new Button("Directory Settings");
        
        scanButton.setOnAction(e -> scanLocalContent());
        cleanupButton.setOnAction(e -> cleanupOrphanedFiles());
        settingsButton.setOnAction(e -> showDirectorySettings());
        
        controls.getChildren().addAll(scanButton, cleanupButton, settingsButton);
        
        parent.getChildren().addAll(statsGrid, controls);
    }
    
    private Tab createLocalModsTab() {
        Tab tab = new Tab("Local Mods");
        tab.setClosable(false);
        
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.7);
        
        // Left side - Mods table
        VBox leftPanel = new VBox(10);
        
        // Table controls
        HBox tableControls = new HBox(10);
        Button refreshButton = new Button("Refresh");
        Button deleteButton = new Button("Delete Selected");
        Button verifyButton = new Button("Verify Integrity");
        ComboBox<String> filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("All Mods", "Installed", "Outdated", "Corrupted");
        filterCombo.setValue("All Mods");
        
        refreshButton.setOnAction(e -> scanLocalContent());
        deleteButton.setOnAction(e -> deleteSelectedMod());
        verifyButton.setOnAction(e -> verifySelectedMod());
        
        tableControls.getChildren().addAll(refreshButton, deleteButton, verifyButton, 
                                          new Separator(), new Label("Filter:"), filterCombo);
        
        // Mods table
        localModsTable = new TableView<>();
        localModsTable.setItems(localMods);
        localModsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldMod, newMod) -> updateModDetails(newMod));
        
        setupModsTableColumns();
        VBox.setVgrow(localModsTable, Priority.ALWAYS);
        
        leftPanel.getChildren().addAll(tableControls, localModsTable);
        
        // Right side - Mod details
        VBox rightPanel = createModDetailsPanel();
        
        splitPane.getItems().addAll(leftPanel, rightPanel);
        tab.setContent(splitPane);
        return tab;
    }
    
    private void setupModsTableColumns() {
        TableColumn<LocalModInfo, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(150);
        
        TableColumn<LocalModInfo, String> versionCol = new TableColumn<>("Version");
        versionCol.setCellValueFactory(new PropertyValueFactory<>("version"));
        versionCol.setPrefWidth(80);
        
        TableColumn<LocalModInfo, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(formatSize(cellData.getValue().getSize())));
        sizeCol.setPrefWidth(80);
        
        TableColumn<LocalModInfo, String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(new PropertyValueFactory<>("path"));
        pathCol.setPrefWidth(200);
        
        TableColumn<LocalModInfo, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        
        TableColumn<LocalModInfo, String> lastModifiedCol = new TableColumn<>("Last Modified");
        lastModifiedCol.setCellValueFactory(cellData -> {
            LocalDateTime lastMod = cellData.getValue().getLastModified();
            return new SimpleStringProperty(lastMod != null ? 
                lastMod.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "Unknown");
        });
        lastModifiedCol.setPrefWidth(120);
        
        localModsTable.getColumns().addAll(nameCol, versionCol, sizeCol, pathCol, statusCol, lastModifiedCol);
    }
    
    private VBox createModDetailsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(0, 0, 0, 15));
        
        Label titleLabel = new Label("Mod Details");
        titleLabel.getStyleClass().add("section-header");
        
        // Details text area
        modDetailsArea = new TextArea();
        modDetailsArea.setEditable(false);
        modDetailsArea.setPrefRowCount(8);
        
        // Files list
        Label filesLabel = new Label("Mod Files");
        filesLabel.getStyleClass().add("subsection-header");
        
        modFilesList = new ListView<>();
        modFilesList.setPrefHeight(200);
        
        // Action buttons
        HBox actions = new HBox(10);
        Button openFolderButton = new Button("Open in Explorer");
        Button checkIntegrityButton = new Button("Check Integrity");
        Button repairButton = new Button("Repair");
        
        openFolderButton.setOnAction(e -> openModFolder());
        checkIntegrityButton.setOnAction(e -> checkModIntegrity());
        repairButton.setOnAction(e -> repairMod());
        
        actions.getChildren().addAll(openFolderButton, checkIntegrityButton, repairButton);
        
        panel.getChildren().addAll(titleLabel, modDetailsArea, filesLabel, modFilesList, actions);
        return panel;
    }
    
    private Tab createLocalModSetsTab() {
        Tab tab = new Tab("Local Mod Sets");
        tab.setClosable(false);
        
        VBox content = new VBox(10);
        
        // Table controls
        HBox controls = new HBox(10);
        Button refreshButton = new Button("Refresh");
        Button deleteButton = new Button("Delete Selected");
        Button exportButton = new Button("Export ModSet");
        
        refreshButton.setOnAction(e -> scanLocalModSets());
        deleteButton.setOnAction(e -> deleteSelectedModSet());
        exportButton.setOnAction(e -> exportSelectedModSet());
        
        controls.getChildren().addAll(refreshButton, deleteButton, exportButton);
        
        // ModSets table
        localModSetsTable = new TableView<>();
        localModSetsTable.setItems(localModSets);
        setupModSetsTableColumns();
        VBox.setVgrow(localModSetsTable, Priority.ALWAYS);
        
        content.getChildren().addAll(controls, localModSetsTable);
        tab.setContent(content);
        return tab;
    }
    
    private void setupModSetsTableColumns() {
        TableColumn<LocalModSetInfo, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(150);
        
        TableColumn<LocalModSetInfo, String> repositoryCol = new TableColumn<>("Repository");
        repositoryCol.setCellValueFactory(new PropertyValueFactory<>("repository"));
        repositoryCol.setPrefWidth(120);
        
        TableColumn<LocalModSetInfo, String> versionCol = new TableColumn<>("Version");
        versionCol.setCellValueFactory(new PropertyValueFactory<>("version"));
        versionCol.setPrefWidth(80);
        
        TableColumn<LocalModSetInfo, String> modCountCol = new TableColumn<>("Mod Count");
        modCountCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.valueOf(cellData.getValue().getModCount())));
        modCountCol.setPrefWidth(80);
        
        TableColumn<LocalModSetInfo, String> sizeCol = new TableColumn<>("Total Size");
        sizeCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(formatSize(cellData.getValue().getTotalSize())));
        sizeCol.setPrefWidth(100);
        
        TableColumn<LocalModSetInfo, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        
        localModSetsTable.getColumns().addAll(nameCol, repositoryCol, versionCol, modCountCol, sizeCol, statusCol);
    }
    
    private Tab createDirectoryManagerTab() {
        Tab tab = new Tab("Directory Manager");
        tab.setClosable(false);
        
        VBox content = new VBox(10);
        
        Label infoLabel = new Label("Manage mod directories that A4Sync will scan for local content.");
        infoLabel.getStyleClass().add("info-text");
        
        // Directory list
        ListView<String> directoriesList = new ListView<>();
        directoriesList.getItems().addAll(config.getModDirectories().stream()
            .map(Path::toString)
            .toList());
        
        // Directory controls
        HBox dirControls = new HBox(10);
        Button addDirButton = new Button("Add Directory");
        Button removeDirButton = new Button("Remove Selected");
        Button scanDirButton = new Button("Scan Selected");
        
        addDirButton.setOnAction(e -> addModDirectory(directoriesList));
        removeDirButton.setOnAction(e -> removeModDirectory(directoriesList));
        scanDirButton.setOnAction(e -> scanSelectedDirectory(directoriesList));
        
        dirControls.getChildren().addAll(addDirButton, removeDirButton, scanDirButton);
        
        content.getChildren().addAll(infoLabel, directoriesList, dirControls);
        VBox.setVgrow(directoriesList, Priority.ALWAYS);
        
        tab.setContent(content);
        return tab;
    }
    
    private HBox createFooterButtons() {
        HBox footer = new HBox(10);
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        Button cleanupButton = new Button("Cleanup All");
        Button settingsButton = new Button("Settings");
        Button closeButton = new Button("Close");
        
        cleanupButton.setOnAction(e -> performFullCleanup());
        settingsButton.setOnAction(e -> showLocalSettings());
        closeButton.setOnAction(e -> close());
        
        footer.getChildren().addAll(cleanupButton, settingsButton, closeButton);
        return footer;
    }
    
    private void scanLocalContent() {
        Task<Void> scanTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    totalModsLabel.setText("Scanning mods...");
                    totalModSetsLabel.setText("Scanning mod sets...");
                });
                
                scanLocalMods();
                scanLocalModSets();
                
                Platform.runLater(() -> {
                    lastScanLabel.setText("Last Scan: " + 
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    updateStatistics();
                });
                
                return null;
            }
        };
        
        new Thread(scanTask).start();
    }
    
    private void scanLocalMods() {
        List<LocalModInfo> mods = new ArrayList<>();
        
        for (Path directory : config.getModDirectories()) {
            if (!Files.exists(directory)) continue;
            
            try (Stream<Path> paths = Files.list(directory)) {
                paths.filter(Files::isDirectory)
                     .filter(p -> p.getFileName().toString().startsWith("@"))
                     .forEach(modPath -> {
                         try {
                             LocalModInfo modInfo = createLocalModInfo(modPath);
                             mods.add(modInfo);
                         } catch (Exception e) {
                             log.warn("Failed to scan mod: " + modPath, e);
                         }
                     });
            } catch (IOException e) {
                log.error("Failed to scan directory: " + directory, e);
            }
        }
        
        Platform.runLater(() -> {
            localMods.clear();
            localMods.addAll(mods);
        });
    }
    
    private LocalModInfo createLocalModInfo(Path modPath) throws IOException {
        LocalModInfo info = new LocalModInfo();
        info.setName(modPath.getFileName().toString());
        info.setPath(modPath.toString());
        
        // Calculate mod size
        long size = Files.walk(modPath)
            .filter(Files::isRegularFile)
            .mapToLong(p -> {
                try {
                    return Files.size(p);
                } catch (IOException e) {
                    return 0;
                }
            }).sum();
        info.setSize(size);
        
        // Get last modified time
        info.setLastModified(LocalDateTime.ofInstant(
            Files.getLastModifiedTime(modPath).toInstant(),
            java.time.ZoneId.systemDefault()));
        
        // Determine status - simplified check for local files
        info.setStatus("Local Only");
        info.setVersion(detectModVersion(modPath));
        
        return info;
    }
    
    private void scanLocalModSets() {
        Task<List<LocalModSetInfo>> scanTask = new Task<List<LocalModSetInfo>>() {
            @Override
            protected List<LocalModSetInfo> call() throws Exception {
                List<LocalModSetInfo> foundModSets = new ArrayList<>();
                
                for (Path directory : config.getModDirectories()) {
                    if (!Files.exists(directory)) continue;
                    
                    updateMessage("Scanning " + directory.getFileName() + "...");
                    
                    try (Stream<Path> paths = Files.walk(directory, 2)) {
                        paths.filter(Files::isDirectory)
                             .filter(path -> !path.equals(directory))
                             .forEach(modSetDir -> {
                                 LocalModSetInfo modSetInfo = scanModSetDirectory(modSetDir);
                                 if (modSetInfo != null) {
                                     foundModSets.add(modSetInfo);
                                 }
                             });
                    } catch (IOException e) {
                        System.err.println("Error scanning directory " + directory + ": " + e.getMessage());
                    }
                }
                
                return foundModSets;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    localModSets.clear();
                    localModSets.addAll(getValue());
                });
            }
        };
        
        Thread scanThread = new Thread(scanTask);
        scanThread.setDaemon(true);
        scanThread.start();
    }
    
    private String detectModVersion(Path modPath) {
        // Try to detect version from various sources
        
        // 1. Check for mod.cpp file (Arma 3 mod definition)
        Path modCppPath = modPath.resolve("mod.cpp");
        if (Files.exists(modCppPath)) {
            String version = extractVersionFromModCpp(modCppPath);
            if (version != null) return version;
        }
        
        // 2. Check for config.cpp file
        Path configCppPath = modPath.resolve("config.cpp");
        if (Files.exists(configCppPath)) {
            String version = extractVersionFromConfigCpp(configCppPath);
            if (version != null) return version;
        }
        
        // 3. Check for meta.cpp file
        Path metaCppPath = modPath.resolve("meta.cpp");
        if (Files.exists(metaCppPath)) {
            String version = extractVersionFromMetaCpp(metaCppPath);
            if (version != null) return version;
        }
        
        // 4. Use last modified date as fallback version
        try {
            FileTime lastModified = Files.getLastModifiedTime(modPath);
            LocalDateTime modTime = LocalDateTime.ofInstant(lastModified.toInstant(), ZoneId.systemDefault());
            return modTime.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        } catch (IOException e) {
            return "Unknown";
        }
    }
    
    private String extractVersionFromModCpp(Path modCppPath) {
        try {
            List<String> lines = Files.readAllLines(modCppPath);
            for (String line : lines) {
                // Look for version patterns like: version = "1.0.0";
                if (line.contains("version") && line.contains("=")) {
                    String[] parts = line.split("=");
                    if (parts.length >= 2) {
                        String version = parts[1].trim()
                            .replaceAll("[\"';]", "")
                            .trim();
                        if (!version.isEmpty()) {
                            return version;
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Ignore and try other methods
        }
        return null;
    }
    
    private String extractVersionFromConfigCpp(Path configCppPath) {
        // Similar to mod.cpp but may have different format
        return extractVersionFromModCpp(configCppPath);
    }
    
    private String extractVersionFromMetaCpp(Path metaCppPath) {
        // Similar to mod.cpp but may have different format  
        return extractVersionFromModCpp(metaCppPath);
    }
    
    private LocalModSetInfo scanModSetDirectory(Path modSetDir) {
        try {
            // Check if this directory contains mods (has subdirectories or .pbo files)
            boolean hasMods = false;
            int modCount = 0;
            
            try (Stream<Path> contents = Files.list(modSetDir)) {
                List<Path> items = contents.collect(Collectors.toList());
                
                for (Path item : items) {
                    if (Files.isDirectory(item) || item.toString().endsWith(".pbo")) {
                        hasMods = true;
                        modCount++;
                    }
                }
            }
            
            if (!hasMods) return null;
            
            LocalModSetInfo info = new LocalModSetInfo();
            info.setName(modSetDir.getFileName().toString());
            info.setPath(modSetDir.toString());
            info.setModCount(modCount);
            info.setLastModified(LocalDateTime.ofInstant(
                Files.getLastModifiedTime(modSetDir).toInstant(),
                ZoneId.systemDefault()));
            
            // Calculate total size
            try {
                long totalSize = Files.walk(modSetDir)
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
                info.setSize(totalSize);
            } catch (IOException e) {
                info.setSize(0);
            }
            
            return info;
            
        } catch (IOException e) {
            return null;
        }
    }
    
    private void updateModDetails(LocalModInfo mod) {
        if (mod == null) {
            modDetailsArea.clear();
            modFilesList.getItems().clear();
            return;
        }
        
        StringBuilder details = new StringBuilder();
        details.append("Name: ").append(mod.getName()).append("\n");
        details.append("Path: ").append(mod.getPath()).append("\n");
        details.append("Size: ").append(formatSize(mod.getSize())).append("\n");
        details.append("Status: ").append(mod.getStatus()).append("\n");
        details.append("Version: ").append(mod.getVersion()).append("\n");
        if (mod.getLastModified() != null) {
            details.append("Last Modified: ").append(
                mod.getLastModified().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        }
        
        modDetailsArea.setText(details.toString());
        
        // Load files list
        Task<List<String>> filesTask = new Task<List<String>>() {
            @Override
            protected List<String> call() throws Exception {
                List<String> files = new ArrayList<>();
                Path modPath = Paths.get(mod.getPath());
                if (Files.exists(modPath)) {
                    Files.walk(modPath)
                         .filter(Files::isRegularFile)
                         .forEach(p -> files.add(modPath.relativize(p).toString()));
                }
                return files;
            }
        };
        
        filesTask.setOnSucceeded(e -> modFilesList.getItems().setAll(filesTask.getValue()));
        new Thread(filesTask).start();
    }
    
    private void updateStatistics() {
        int totalMods = localMods.size();
        int totalModSets = localModSets.size();
        long totalSize = localMods.stream().mapToLong(LocalModInfo::getSize).sum();
        
        totalModsLabel.setText("Total Mods: " + totalMods);
        totalModSetsLabel.setText("Total Mod Sets: " + totalModSets);
        totalSizeLabel.setText("Total Size: " + formatSize(totalSize));
    }
    
    private void deleteSelectedMod() {
        LocalModInfo selected = localModsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Mod");
        confirm.setHeaderText("Delete " + selected.getName() + "?");
        confirm.setContentText("This will permanently delete the mod files from your system.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // TODO: Implement mod deletion
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setContentText("Mod deletion feature coming soon!");
                info.showAndWait();
            }
        });
    }
    
    private void addModDirectory(ListView<String> directoriesList) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Mod Directory");
        
        File selectedDir = chooser.showDialog(this);
        if (selectedDir != null) {
            Path newPath = selectedDir.toPath();
            config.addModDirectory(newPath);
            directoriesList.getItems().add(newPath.toString());
        }
    }
    
    private void removeModDirectory(ListView<String> directoriesList) {
        String selected = directoriesList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            config.removeModDirectory(Paths.get(selected));
            directoriesList.getItems().remove(selected);
        }
    }
    
    private void scanSelectedDirectory(ListView<String> directoriesList) {
        String selected = directoriesList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // TODO: Implement selective directory scanning
            scanLocalContent();
        }
    }
    
    // Placeholder methods for features to be implemented
    private void verifySelectedMod() { showNotImplemented("Mod verification"); }
    private void openModFolder() { showNotImplemented("Open mod folder"); }
    private void checkModIntegrity() { showNotImplemented("Mod integrity check"); }
    private void repairMod() { showNotImplemented("Mod repair"); }
    private void deleteSelectedModSet() { showNotImplemented("ModSet deletion"); }
    private void exportSelectedModSet() { showNotImplemented("ModSet export"); }
    private void cleanupOrphanedFiles() { showNotImplemented("Cleanup orphaned files"); }
    private void showDirectorySettings() { showNotImplemented("Directory settings"); }
    private void performFullCleanup() { showNotImplemented("Full cleanup"); }
    private void showLocalSettings() { showNotImplemented("Local settings"); }
    
    private void showNotImplemented(String feature) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Feature Not Implemented");
        info.setContentText(feature + " feature coming soon!");
        info.showAndWait();
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    // Inner classes for data models
    public static class LocalModInfo {
        private String name;
        private String version = "Unknown";
        private String path;
        private String status;
        private long size;
        private LocalDateTime lastModified;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        
        public LocalDateTime getLastModified() { return lastModified; }
        public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
    }
    
    public static class LocalModSetInfo {
        private String name;
        private String repository;
        private String version;
        private int modCount;
        private long totalSize;
        private String status;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getRepository() { return repository; }
        public void setRepository(String repository) { this.repository = repository; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public int getModCount() { return modCount; }
        public void setModCount(int modCount) { this.modCount = modCount; }
        
        public long getTotalSize() { return totalSize; }
        public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}