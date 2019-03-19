package gui;

import comm.Client;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import struct.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class InsertController implements Initializable {
    public Label tableNameLabel;
    public TableView tableView;
    public Button cancelButton;
    public Button insertButton;
    public Button addButton;
    private String tableName;
    private String databaseName;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        tableView.setEditable(true);
    }

    public void closePopup(ActionEvent actionEvent) {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    public void insertIntoTable(ActionEvent actionEvent) {
        if (tableView.getItems().size() == 0) {
            return;
        }

        Client.getClient().write("USE " + databaseName + "\n");
        String answer = Client.getClient().readLine();

        if (!answer.equals("OK")) {
            Alert alert = new Alert(Alert.AlertType.ERROR, answer);
            alert.setTitle("Error Dialog");
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        for (int i = 0; i < tableView.getItems().size(); ++i) {
            String sql = "INSERT INTO " + tableName + " VALUES ( ";
            ObservableList<String> row = (ObservableList<String>) tableView.getItems().get(i);

            for (int j = 0; j < row.size() - 1; ++j) {
                sql += row.get(j) + " , ";
            }

            sql += row.get(row.size() - 1) + " )\n";
            System.out.println(sql);
            Client.getClient().write(sql);
            answer = Client.getClient().readLine();

            if (!answer.equals("OK")) {
                Alert alert = new Alert(Alert.AlertType.ERROR, answer);
                alert.setTitle("Error Dialog");
                alert.setHeaderText(null);
                alert.showAndWait();
            } else {
                tableView.getItems().remove(i);
            }
        }

        if (tableView.getItems().size() == 0) {
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        }
    }

    public void addRowToView(ActionEvent actionEvent) {
        ObservableList<String> row = FXCollections.observableArrayList();

        for (int i = 0; i < tableView.getColumns().size(); ++i) {
            row.add("");
        }

        tableView.getItems().add(row);
    }

    public void load(String tableName, String databaseName) {
        this.tableName = tableName;
        this.databaseName = databaseName;

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                tableNameLabel.setText(tableName);
                init();
            }
        });
    }

    private void init() {
        Databases databases = Client.getClient().getDatabases();

        if (databases == null) {
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
            return;
        }

        Structure structure = table.getTableStructure();
        List<Attribute> attributeList = structure.getAttributeList();
        int index = 0;

        for (Attribute attr : attributeList) {
            final int aux = index;
            TableColumn<ObservableList<String>, String> column = new TableColumn<>(attr.getName() + " [ " + attr.getType() + " ]");

            column.setCellValueFactory(p -> Bindings.stringValueAt(p.getValue(), aux));
            column.setCellFactory(TextFieldTableCell.forTableColumn());
            column.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).set(aux, t.getNewValue()));

            tableView.getColumns().add(column);
            ++index;
        }
    }
}
