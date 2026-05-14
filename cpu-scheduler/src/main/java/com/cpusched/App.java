package com.cpusched;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("primary"), 800, 900);
        stage.setTitle("CPU Scheduling Simulator");
        stage.setMinWidth(800);
        stage.setMinHeight(900);
                try {
            Image icon = new Image(App.class.getResourceAsStream("app-icon.jpg"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            // Icon not found, continue without it
        }
        stage.setScene(scene);
        stage.show();
    }

    // Switches the root FXML view (navigation)
    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    // Loads an FXML file from resources
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();

    }
}