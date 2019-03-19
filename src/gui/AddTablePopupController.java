package gui;

import comm.Client;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AddTablePopupController implements Initializable {
    private String databaseName;
    public Label tableNameLabel;
    public Label columnNameLabel;
    public Label typeNameLabel;
    public TextField tableNameField;
    public TableView tableData;
    public TextField columnName;
    public CheckBox primaryKey;
    public TextField foreignKey;
    public CheckBox isUnique;
    public CheckBox notNull;
    public Button submitButton;
    public Button addButton;
    public Button cancelButton;
    public TableColumn nameColumn;
    public TableColumn typeColumn;
    public TableColumn primaryKeyColumn;
    public TableColumn foreignKeyColumn;
    public TableColumn uniqueColumn;
    public TableColumn notNullColumn;
    public TextField typeField;

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    private void clearFields() {
        columnName.setText("");
        typeField.setText("");
        primaryKey.setSelected(false);
        foreignKey.setText("");
        isUnique.setSelected(false);
        notNull.setSelected(false);
    }

    public void updateView(ActionEvent actionEvent) {
        if (!validate()) {
            return;
        }

        TableRow tableRow = new TableRow();
        tableRow.setColumnName(columnName.getText());
        tableRow.setType(typeField.getText());

        if (primaryKey.isSelected()) {
            tableRow.setPrimaryKey("True");
        } else {
            tableRow.setPrimaryKey("False");
        }

        tableRow.setForeignKey(foreignKey.getText());

        if (isUnique.isSelected()) {
            tableRow.setUnique("True");
        } else {
            tableRow.setUnique("False");
        }

        if (notNull.isSelected()) {
            tableRow.setNotNull("True");
        } else {
            tableRow.setNotNull("False");
        }

        tableData.getItems().add(tableRow);
        clearFields();
    }

    public void addToTable(ActionEvent actionEvent) {
        if (tableData.getItems().size() == 0) {
            return;
        }

        String tableName = tableNameField.getText();
        String sql = "CREATE TABLE " + tableName + " ( ";

        for (int i = 0; i < tableData.getItems().size() - 1; ++i) {
            TableRow tableRow = (TableRow) tableData.getItems().get(i);
            sql += tableRow.getColumnName() + " " + tableRow.getType() + " ";

            if (tableRow.getPrimaryKey().equals("True")) {
                sql += "PK";
            }
            if (!tableRow.getForeignKey().equals("")) {
                sql += "FK " + tableRow.getForeignKey();
            }
            if (tableRow.getUnique().equals("True")) {
                sql += " UNIQUE";
            }
            if (tableRow.getNotNull().equals("True")) {
                sql += " ISNULL";
            }

            sql += " , ";
        }

        TableRow tableRow = (TableRow) tableData.getItems().get(tableData.getItems().size() - 1);
        sql += tableRow.getColumnName() + " " + tableRow.getType() + " ";

        if (tableRow.getPrimaryKey().equals("True")) {
            sql += "PK";
        }
        if (!tableRow.getForeignKey().equals("")) {
            sql += "FK " + tableRow.getForeignKey();
        }
        if (tableRow.getUnique().equals("True")) {
            sql += " UNIQUE";
        }
        if (tableRow.getNotNull().equals("True")) {
            sql += " ISNULL";
        }

        sql += " )";

        Client.getClient().write("USE " + databaseName + "\n");
        String answer = Client.getClient().readLine();

        if (answer.equals("OK")) {
            Client.getClient().write(sql + "\n");
            answer = Client.getClient().readLine();
        }

        if (answer.equals("OK")) {
            Stage stage = (Stage) addButton.getScene().getWindow();
            stage.close();
        } else {
            clearFields();

            tableNameLabel.setVisible(false);
            columnNameLabel.setVisible(false);
            typeNameLabel.setVisible(false);

            Alert alert = new Alert(Alert.AlertType.ERROR, answer);
            alert.setTitle("Error Dialog");
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }

    private boolean validate() {
        boolean ok = true;

        if (tableNameField.getText().isEmpty()) {
            tableNameLabel.setVisible(true);
            ok = false;
        } else {
            tableNameLabel.setVisible(false);
        }

        if (columnName.getText().isEmpty()) {
            columnNameLabel.setVisible(true);
            ok = false;
        } else {
            columnNameLabel.setVisible(false);
        }

        if (typeField.getText().isEmpty()) {
            typeNameLabel.setVisible(true);
            ok = false;
        } else {
            typeNameLabel.setVisible(false);
        }

        return ok;
    }

    public void closePopup(ActionEvent actionEvent) {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        tableData.setEditable(true);

        nameColumn.setCellValueFactory(new PropertyValueFactory<TableRow, String>("columnName"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<TableRow, String>("type"));
        primaryKeyColumn.setCellValueFactory(new PropertyValueFactory<TableRow, String>("primaryKey"));
        foreignKeyColumn.setCellValueFactory(new PropertyValueFactory<TableRow, String>("foreignKey"));
        uniqueColumn.setCellValueFactory(new PropertyValueFactory<TableRow, String>("unique"));
        notNullColumn.setCellValueFactory(new PropertyValueFactory<TableRow, String>("notNull"));

        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColumn.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent>() {
            @Override
            public void handle(TableColumn.CellEditEvent cellEditEvent) {
                ((TableRow) cellEditEvent.getTableView().getItems().get(cellEditEvent.getTablePosition().getRow())).setColumnName(cellEditEvent.getNewValue().toString());
            }
        });

        typeColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        typeColumn.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent>() {
            @Override
            public void handle(TableColumn.CellEditEvent cellEditEvent) {
                ((TableRow) cellEditEvent.getTableView().getItems().get(cellEditEvent.getTablePosition().getRow())).setType(cellEditEvent.getNewValue().toString());
            }
        });

        primaryKeyColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        primaryKeyColumn.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent>() {
            @Override
            public void handle(TableColumn.CellEditEvent cellEditEvent) {
                ((TableRow) cellEditEvent.getTableView().getItems().get(cellEditEvent.getTablePosition().getRow())).setPrimaryKey(cellEditEvent.getNewValue().toString());
            }
        });

        foreignKeyColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        foreignKeyColumn.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent>() {
            @Override
            public void handle(TableColumn.CellEditEvent cellEditEvent) {
                ((TableRow) cellEditEvent.getTableView().getItems().get(cellEditEvent.getTablePosition().getRow())).setForeignKey(cellEditEvent.getNewValue().toString());
            }
        });

        uniqueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        uniqueColumn.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent>() {
            @Override
            public void handle(TableColumn.CellEditEvent cellEditEvent) {
                ((TableRow) cellEditEvent.getTableView().getItems().get(cellEditEvent.getTablePosition().getRow())).setUnique(cellEditEvent.getNewValue().toString());
            }
        });

        notNullColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        notNullColumn.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent>() {
            @Override
            public void handle(TableColumn.CellEditEvent cellEditEvent) {
                ((TableRow) cellEditEvent.getTableView().getItems().get(cellEditEvent.getTablePosition().getRow())).setNotNull(cellEditEvent.getNewValue().toString());
            }
        });
    }
}
