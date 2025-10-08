package com.a4sync.client;

import com.a4sync.client.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class A4SyncClientApplication extends Application {
    private static final Logger log = LoggerFactory.getLogger(A4SyncClientApplication.class);

    private MainController mainController;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(A4SyncClientApplication.class.getResource("/fxml/main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1024, 768);
        this.mainController = fxmlLoader.getController(); // Get controller reference
        stage.setTitle("A4Sync - Arma 4 Mod Manager");
        stage.setScene(scene);
        stage.show();
        
        log.info("A4Sync Client started successfully");
    }


    public static void main(String[] args) {
        launch(args);
    }
}
