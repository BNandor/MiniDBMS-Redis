package gui;

import comm.Client;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

import java.io.IOException;

public class TableContextMenu extends ContextMenu {
    public TableContextMenu(String databaseName, String tableName) {
        MenuItem dropTable = new MenuItem("Drop Table");
        MenuItem insertIntoTable = new MenuItem("Insert");
        MenuItem createIndex = new MenuItem("Create Index");

        dropTable.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Alert dialog = new Alert(Alert.AlertType.CONFIRMATION, "Drop table [" + tableName + "]?",
                        ButtonType.YES, ButtonType.CANCEL);
                dialog.showAndWait();

                if (dialog.getResult() == ButtonType.YES) {
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

        createIndex.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("createIndex.fxml"));
                try {
                    Scene scene = new Scene(loader.load());
                    Stage stage = new Stage();
                    stage.setScene(scene);
                    stage.setMinWidth(500);
                    stage.setMinHeight(150);

                    CreateIndexController createIndexController = loader.getController();
                    createIndexController.load(tableName, databaseName);

                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        this.getItems().addAll(insertIntoTable, createIndex, dropTable);
    }
}
