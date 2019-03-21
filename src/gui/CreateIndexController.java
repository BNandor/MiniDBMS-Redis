package gui;

import comm.Client;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import struct.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CreateIndexController implements Initializable {
    private String tableName;
    private String databaseName;
    public Label tableNameLabel;
    public ComboBox comboBox;
    public Button cancelButton;
    public Button addButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    private void exit() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    public void closeWindow(ActionEvent actionEvent) {
        exit();
    }

    public void createIndex(ActionEvent actionEvent) {
        Client.getClient().write("USE " + databaseName + "\n");
        String answer = Client.getClient().readLine();

        if (!answer.equals("OK")) {
            Alert alert = new Alert(Alert.AlertType.ERROR, answer);
            alert.setTitle("Error Dialog");
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        String columnName = comboBox.getSelectionModel().getSelectedItem().toString();
        Client.getClient().write("CREATE INDEX " + tableName + "." + columnName + "\n");
        answer = Client.getClient().readLine();

        if (!answer.equals("OK")) {
            Alert alert = new Alert(Alert.AlertType.ERROR, answer);
            alert.setTitle("Error Dialog");
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        } else {
            exit();
        }
    }

    public void load(String tableName, String databaseName) {
        this.tableName = tableName;
        this.databaseName = databaseName;

        Platform.runLater(() -> {
            tableNameLabel.setText(tableName);
            setup();
        });
    }

    private void setup() {
        Databases databases = Client.getClient().getDatabases();

        if (databases == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Databases does not exist.");
            alert.setTitle("Error Dialog");
            alert.setHeaderText(null);
            alert.showAndWait();
            exit();
            return;
        }

        List<Database> databaseList = databases.getDatabaseList();
        Database database = null;

        for (Database db : databaseList) {
            if (db.getDatabaseName().equals(this.databaseName)) {
                database = db;
                break;
            }
        }

        if (database == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Database [ " + this.databaseName + " ] does not exist.");
            alert.setTitle("Error Dialog");
            alert.setHeaderText(null);
            alert.showAndWait();
            exit();
            return;
        }

        Tables tables = database.getTables();
        List<Table> tableList = tables.getTableList();
        Table table = null;

        for (Table t : tableList) {
            if (t.getTableName().equals(this.tableName)) {
                table = t;
            }
        }

        if (table == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Table [ " + this.tableName + " ] does not exist.");
            alert.setTitle("Error Dialog");
            alert.setHeaderText(null);
            alert.showAndWait();
            exit();
            return;
        }

        Structure structure = table.getTableStructure();
        List<Attribute> attributeList = structure.getAttributeList();

        for (Attribute attr : attributeList) {
            comboBox.getItems().add(attr.getName());
        }
    }
}
