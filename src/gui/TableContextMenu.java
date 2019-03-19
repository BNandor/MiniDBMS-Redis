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

public class TableContextMenu extends ContextMenu {
    public TableContextMenu(String databaseName, String tableName) {
        MenuItem dropTable = new MenuItem("Drop Table");
        MenuItem insertIntoTable = new MenuItem("Insert");

        dropTable.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Client.getClient().write("USE " + databaseName + "\n");
                String answer = Client.getClient().readLine();

                if (answer.equals("OK")) {
                    Client.getClient().write("DROP TABLE " + tableName + "\n");
                    answer = Client.getClient().readLine();
                }

                if (!answer.equals("OK")) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, answer);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.showAndWait();
                }
            }
        });

        insertIntoTable.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("insertPopup.fxml"));
                try {
                    Scene scene = new Scene(loader.load());
                    Stage stage = new Stage();
                    stage.setScene(scene);
                    stage.setMinWidth(700);
                    stage.setMinHeight(500);

                    InsertController insertController = loader.getController();
                    insertController.load(tableName, databaseName);

                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        this.getItems().addAll(dropTable, insertIntoTable);
    }
}
