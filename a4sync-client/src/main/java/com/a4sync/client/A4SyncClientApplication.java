package com.a4sync.client;

import com.a4sync.client.controller.MainController;
import com.a4sync.client.service.RepositoryManager;
import com.a4sync.client.service.RepositoryManagerFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class A4SyncClientApplication extends Application {
    private static final Logger log = LoggerFactory.getLogger(A4SyncClientApplication.class);

    @Override
    public void init() {
        // Initialize repository manager
        Path appDir = Path.of(System.getProperty("user.home"), ".a4sync");
        RepositoryManagerFactory.initialize(appDir);
    }

    private MainController mainController;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(A4SyncClientApplication.class.getResource("/fxml/main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1024, 768);
        this.mainController = fxmlLoader.getController(); // Get controller reference
        stage.setTitle("A4Sync - Arma 4 Mod Manager");
        stage.setScene(scene);
        stage.show();
        
        // Check repositories after UI is shown
        checkRepositories();
    }
    
    private void checkRepositories() {
        RepositoryManagerFactory.getInstance()
            .checkRepositories()
            .thenAcceptAsync(statuses -> {
                // Filter only updates and not downloaded
                List<RepositoryManager.ModSetStatus> important = statuses.stream()
                    .filter(s -> s.getStatus() != RepositoryManager.ModSetStatus.Status.UP_TO_DATE)
                    .toList();
                
                if (!important.isEmpty()) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Repository Updates");
                        alert.setHeaderText("Mod Updates Available");
                        
                        StringBuilder content = new StringBuilder();
                        content.append("The following mod sets need attention:\n\n");
                        
                        for (RepositoryManager.ModSetStatus status : important) {
                            content.append(status.getRepository().getName())
                                .append(" - ")
                                .append(status.getRemoteSet().getName())
                                .append(": ");
                                
                            if (status.getStatus() == RepositoryManager.ModSetStatus.Status.NOT_DOWNLOADED) {
                                content.append("Not downloaded\n");
                            } else {
                                content.append("Update available (")
                                    .append(status.getLocalSet().getVersion())
                                    .append(" → ")
                                    .append(status.getRemoteSet().getVersion())
                                    .append(")\n");
                            }
                        }
                        
                        alert.setContentText(content.toString());
                        
                        Optional<ButtonType> result = alert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            // Navigate to repository management view
                            if (mainController != null) {
                                mainController.navigateToRepositoryManagement();
                            }
                            log.info("User acknowledged updates - navigated to repository management tab");
                        }
                    });
                } else {
                    log.info("No updates found in repositories");
                }
            })
            .exceptionally(e -> {
                log.error("Failed to check repositories", e);
                return null;
            });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
