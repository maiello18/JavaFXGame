package Client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    public ClientManager manager;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        manager = new ClientManager(primaryStage);
    }
    public static void main(String[] args) {
        launch(args);
    }

    public void stop() {
        manager.getConnection().killConnection();
    }
}
