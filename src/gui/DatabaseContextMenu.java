package gui;

import comm.Client;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

import java.io.IOException;

public class DatabaseContextMenu extends ContextMenu {
    public DatabaseContextMenu(String databaseName) {
        MenuItem addTableMenu = new MenuItem("Add Table");
        MenuItem deleteTableMenu = new MenuItem("Drop Database");

        addTableMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("addTablePopup.fxml"));
                try {
                    Scene scene = new Scene(loader.load());
                    Stage stage = new Stage();
                    stage.setScene(scene);
                    stage.setMinWidth(700);
                    stage.setMinHeight(700);

                    AddTablePopupController tablePopupController = loader.getController();
                    tablePopupController.setDatabaseName(databaseName);

                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        deleteTableMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Client.getClient().write("DROP DATABASE " + databaseName + "\n");
                String answer = Client.getClient().readLine();

                if (!answer.equals("OK")) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, answer);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.showAndWait();
                }
            }
        });

        getItems().addAll(addTableMenu, deleteTableMenu);
    }
}
