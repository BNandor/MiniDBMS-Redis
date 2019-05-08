package gui;

import comm.Client;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("userInterface.fxml"));
        Parent root = loader.load();

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                Client.getClient().write("exit\n");
            }
        });

        primaryStage.setTitle("MiniDBMS");
        primaryStage.setScene(new Scene(root));
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(630);
        primaryStage.show();

        Controller controller = loader.getController();
        controller.setup();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
