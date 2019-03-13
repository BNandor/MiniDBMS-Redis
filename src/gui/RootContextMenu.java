package gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

import java.io.IOException;

public class RootContextMenu extends ContextMenu {
    public RootContextMenu() {
        MenuItem addDatabase = new MenuItem("Add Database");

        addDatabase.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("addDatabasePopup.fxml"));
                try {
                    Scene scene = new Scene(loader.load());
                    Stage stage = new Stage();
                    stage.setScene(scene);
                    stage.setMinWidth(400);
                    stage.setMinHeight(180);
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        this.getItems().add(addDatabase);
    }
}
