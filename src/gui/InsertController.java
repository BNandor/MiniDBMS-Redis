package gui;

import comm.Client;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import struct.*;

import java.util.List;

public class InsertController {
    public Label tableNameLabel;
    public TableView tableView;
    public Button cancelButton;
    public Button insertButton;
    public Button addButton;

    private String tableName;
    private String databaseName;

    public void closePopup(ActionEvent actionEvent) {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    public void insertIntoTable(ActionEvent actionEvent) {
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
            String sql = "INSERT INTO " + tableName + " values ( ";
            ObservableList<String> row = (ObservableList<String>) tableView.getItems().get(i);

            for (int j = 0; j < row.size() - 1; ++j) {
                sql += row.get(j) + " , ";
            }

            sql += row.get(row.size() - 1) + " )\n";
            Client.getClient().write(sql);
            answer = Client.getClient().readLine();

            if (!answer.equals("OK")) {
                Alert alert = new Alert(Alert.AlertType.ERROR, answer);
                alert.setTitle("Error Dialog");
                alert.setHeaderText(null);
                alert.showAndWait();
            }
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

        int index = 0;
        Structure structure = table.getTableStructure();
        List<Attribute> attributeList = structure.getAttributeList();

        for (Attribute attr : attributeList) {
            final int tmp = index;
            TableColumn column = new TableColumn(attr.getName() + " ( " + attr.getType() + " )");

            column.setCellFactory(TextFieldTableCell.forTableColumn());
            column.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent>() {
                @Override
                public void handle(TableColumn.CellEditEvent cellEditEvent) {
                    ((ObservableList<String>) cellEditEvent.getTableView().getItems().get(cellEditEvent.getTablePosition().getRow())).set(tmp, cellEditEvent.getNewValue().toString());
                }
            });

            tableView.getColumns().add(column);
            ++index;
        }
    }
}
