package gui;

import comm.Client;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class TableContextMenu extends ContextMenu {
    public TableContextMenu(String databaseName, String tableName) {
        MenuItem dropTable = new MenuItem("Drop Table");

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

        this.getItems().add(dropTable);
    }
}
