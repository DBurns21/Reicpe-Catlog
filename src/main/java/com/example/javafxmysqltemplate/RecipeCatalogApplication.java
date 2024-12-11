package com.example.javafxmysqltemplate;

import com.example.database.Database;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class RecipeCatalogApplication extends Application {
    private static final String CONFIG_FILE_PATH = "config.properties";

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(RecipeCatalogController.class.getResource("signIn-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Recipe Catalog");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        try {
            Database.initialize(CONFIG_FILE_PATH);
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize database", e);
        }
        launch();
    }

}